package com.application.execution

import com.application.config.DockerConfiguration
import com.application.config.ExecutionProperties
import com.application.schema.TestDriverType
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@Timeout(30)
class JsonTestDriverExecutionTest {
  @TempDir
  lateinit var executionRoot: Path

  private val docker = DockerConfiguration().dockerClient()

  @AfterEach
  fun closeDockerClient() {
    docker.close()
  }

  @Test
  fun `typed drivers share one instance and JVM across cases`() {
    val source = """
      private var calls = 0L
      private val pid = ProcessHandle.current().pid()
      fun solve(input: Long): Long {
        check(ProcessHandle.current().pid() == pid)
        return input + calls++
      }
    """.trimIndent()
    val driver = """
      import com.application.execution.JsonTestDriver

      class TestDriver : JsonTestDriver<Long, Long>(Long::class) {
        private var calls = 0L
        override fun run(input: Long): Long = solve(input) + calls++
      }
    """.trimIndent()

    val result = executeDriver(driver, List(3) { JsonPrimitive(1) }, source)
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.SUCCEEDED, completed.status)
    assertTrue(completed.caseResults.all { it.status == ProgramStatus.SUCCEEDED }, completed.toString())
    assertEquals(listOf(1, 3, 5).map(::JsonPrimitive), completed.caseResults.map { it.outputJson })
  }

  @Test
  fun `typed drivers construct submitted classes using case inputs`() {
    val source = """
      class RunningTotal(initial: Long) {
        private var total = initial

        fun add(value: Long): Long {
          total += value
          return total
        }
      }
    """.trimIndent()
    val driver = """
      import com.application.execution.JsonTestDriver

      data class Input(val initial: Long, val values: List<Long>)

      class TestDriver : JsonTestDriver<Input, List<Long>>(Input::class) {
        override fun run(input: Input): List<Long> {
          val total = RunningTotal(input.initial)
          return input.values.map { total.add(it) }
        }
      }
    """.trimIndent()
    val inputs = listOf(
      Json.parseToJsonElement("""{"initial":9007199254740993,"values":[1,2]}"""),
      Json.parseToJsonElement("""{"initial":10,"values":[3,-1]}"""),
    )

    val result = executeDriver(driver, inputs, source)
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.SUCCEEDED, completed.status)
    assertTrue(completed.caseResults.all { it.status == ProgramStatus.SUCCEEDED }, completed.toString())
    assertEquals(
      listOf(
        Json.parseToJsonElement("[9007199254740994,9007199254740996]"),
        Json.parseToJsonElement("[13,12]"),
      ),
      completed.caseResults.map { it.outputJson },
    )
  }

  @Test
  fun `typed failures and output overflows retain later case results`() {
    val driver = """
      import com.application.execution.JsonTestDriver

      class TestDriver : JsonTestDriver<Int, String?>(Int::class) {
        override fun run(input: Int): String? = when (input) {
          0 -> error("failed invocation")
          1 -> "x".repeat(20_001)
          else -> null
        }
      }
    """.trimIndent()

    val result = executeDriver(driver, listOf(JsonPrimitive("invalid")) + listOf(0, 1, 2).map(::JsonPrimitive))
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.SUCCEEDED, completed.status)
    assertEquals(
      listOf(ProgramStatus.FAILED, ProgramStatus.FAILED, ProgramStatus.OUTPUT_LIMIT_EXCEEDED, ProgramStatus.SUCCEEDED),
      completed.caseResults.map { it.status },
    )
    assertTrue(completed.caseResults[1].stderr.contains("IllegalStateException"))
    assertEquals(20_000, completed.caseResults[2].stdout.toByteArray(Charsets.UTF_8).size)
    assertEquals(JsonNull, completed.caseResults.last().outputJson)
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `typed driver construction and execution are covered by the case deadline`(loopInConstructor: Boolean) {
    val driver = """
      import com.application.execution.JsonTestDriver

      class TestDriver : JsonTestDriver<Int, Int>(Int::class) {
        init {
          if ($loopInConstructor) {
            while (true) { Thread.onSpinWait() }
          }
        }
        override fun run(input: Int): Int {
          while (true) { Thread.onSpinWait() }
        }
      }
    """.trimIndent()

    val result = executeDriver(driver, listOf(1, 2).map(::JsonPrimitive))
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.TIME_LIMIT_EXCEEDED, completed.status)
    assertEquals(1, completed.caseResults.size)
    assertEquals(ProgramStatus.TIME_LIMIT_EXCEEDED, completed.caseResults.single().status)
  }

  @ParameterizedTest
  @CsvSource(
    "class DifferentDriver | ClassNotFoundException",
    "class TestDriver | ClassCastException",
    "class TestDriver(val argument: Int) : JsonTestDriver<Int, Int>(Int::class) { override fun run(input: Int) = input } | NoSuchMethodException",
    delimiter = '|',
  )
  fun `typed entry points must implement the contract and have a public no-argument constructor`(declaration: String, failure: String) {
    val driver = """
      import com.application.execution.JsonTestDriver
      $declaration
    """.trimIndent()

    val result = executeDriver(driver, listOf(JsonPrimitive(1)))
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.FAILED, completed.caseResults.single().status)
    assertTrue(completed.caseResults.single().stderr.contains(failure), completed.toString())
  }

  @Test
  fun `main function mode remains available explicitly`() {
    val result = executeDriver(
      driver = "fun main(args: Array<String>) { print(System.`in`.bufferedReader().readText()) }",
      inputs = listOf(JsonPrimitive(42)),
      type = TestDriverType.MAIN_FUNCTION,
    )
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.SUCCEEDED, completed.caseResults.single().status)
    assertEquals(JsonPrimitive(42), completed.caseResults.single().outputJson)
  }

  @ParameterizedTest
  @EnumSource(TestDriverType::class)
  fun `published driver examples compile and execute`(type: TestDriverType) {
    val documentation = KotlinLanguageExecutionConfig().testDriverTypes.single { it.type == type }
    val driver = documentation.documentationMarkdown.substringAfter("```kotlin\n").substringBefore("\n```")
    val source = "class Solution { fun sum(numbers: List<Int>): Int = numbers.sum() }"

    val result = executeDriver(
      driver = driver,
      inputs = listOf(Json.parseToJsonElement("""{"numbers":[1,2,3]}""")),
      source = source,
      type = type,
    )
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.SUCCEEDED, completed.caseResults.single().status, completed.toString())
    assertEquals(JsonPrimitive(6), completed.caseResults.single().outputJson)
  }

  private fun executeDriver(
    driver: String,
    inputs: List<JsonElement>,
    source: String = "",
    type: TestDriverType = TestDriverType.JSON_TEST_DRIVER,
  ): CodeExecutionResult {
    val program = KotlinLanguageExecutionConfig().prepare(source, driver, type)
    val service = DockerExecutionService(ExecutionProperties(workspaceDirectory = executionRoot.toString()), docker)

    val result = service.executeCode("typed-driver-test", DockerExecutionServiceTest.IMAGE, program, inputs, 2_000, 256)

    Files.list(executionRoot).use { assertEquals(0, it.count()) }
    val containers = docker.listContainersCmd().withShowAll(true)
      .withLabelFilter(mapOf("interview-forge.execution-id" to "typed-driver-test"))
      .exec()
    assertTrue(containers.isEmpty(), "Execution left a container behind")

    return result
  }
}

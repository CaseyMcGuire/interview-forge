package com.application.execution

import com.application.config.ExecutionProperties
import com.application.config.DockerConfiguration
import com.github.dockerjava.api.exception.NotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull

@Timeout(30)
class DockerExecutionServiceTest {
  @TempDir
  lateinit var executionRoot: Path

  private val docker = DockerConfiguration().dockerClient()

  private fun executionService() = DockerExecutionService(
    ExecutionProperties(workspaceDirectory = executionRoot.toString()),
    docker,
  )

  @AfterEach
  fun closeDockerClient() {
    docker.close()
  }

  @Test
  fun `missing images are unavailable and failed creation is cleaned up`() {
    val executionService = executionService()
    val missingImage = "interview-forge-missing:${UUID.randomUUID()}"
    assertFalse(executionService.isAvailable(missingImage))

    assertThrows(NotFoundException::class.java) {
      executionService.executeCode(
        "test-123", missingImage, PreparedProgram(emptyMap(), null, listOf("true")),
        listOf(JsonNull), 2_000, 128,
      )
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `the complete suite shares one JVM and preserves global state`() {
    val source = """
      var calls = 0
      val initialPid = ProcessHandle.current().pid()
      fun solve(input: Int): Int {
        check(ProcessHandle.current().pid() == initialPid)
        return input + calls++
      }
      fun main(args: Array<String>) { error("The solution main must not run") }
    """.trimIndent()

    val program = KotlinLanguageExecutionConfig().prepare(
      source,
      "fun main() = println(solve(System.`in`.bufferedReader().readText().toInt()))",
    )
    val result = executionService().executeCode("test-123", IMAGE, program, List(3) { JsonPrimitive(1) }, 1_000, 128)
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result)

    assertEquals(ProgramStatus.SUCCEEDED, completed.status)
    assertEquals(listOf(1, 2, 3).map(::JsonPrimitive), completed.caseResults.map { it.outputJson })
    assertEquals(List(3) { JsonPrimitive(1) }, completed.caseResults.map { it.inputJson })
    assertEquals(listOf("1\n", "2\n", "3\n"), completed.caseResults.map { it.stdout })
    assertTrue(completed.caseResults.all { it.status == ProgramStatus.SUCCEEDED && it.runtimeMs != null })
    assertTrue(completed.caseResults.all { it.runtimeMs!! >= 0 })

    assertExecutionCleanedUp()
  }

  @Test
  fun `drivers use Jackson to read and write Kotlin data classes across multiple cases`() {
    val driver = """
      import tools.jackson.module.kotlin.jacksonObjectMapper
      import tools.jackson.module.kotlin.readValue

      data class Input(val amount: Long, val labels: List<String>, val note: String?, val enabled: Boolean)

      private val mapper = jacksonObjectMapper()

      fun main() {
        val input = mapper.readValue<Input>(System.`in`.bufferedReader().readText())
        print(mapper.writeValueAsString(solve(input)))
      }
    """.trimIndent()
    val inputs = listOf(
      Json.parseToJsonElement("""{"amount":9007199254740993,"labels":["héllo 世界","quote\""],"note":null,"enabled":true}"""),
      Json.parseToJsonElement("""{"amount":-42,"labels":[],"note":"another case","enabled":false}"""),
    )
    val program = KotlinLanguageExecutionConfig().prepare("fun solve(input: Input): Input = input", driver)

    val result = executionService().executeCode("test-123", IMAGE, program, inputs, 2_000, 256)
    val completed = assertInstanceOf(CodeExecutionResult.Completed::class.java, result, result.toString())

    assertEquals(ProgramStatus.SUCCEEDED, completed.status)
    assertTrue(completed.caseResults.all { it.status == ProgramStatus.SUCCEEDED }, completed.toString())
    assertEquals(inputs, completed.caseResults.map { it.outputJson })

    assertExecutionCleanedUp()
  }

  @Test
  fun `wrong answers do not stop later cases in the same JVM`() {
    val source = """
      var calls = 0
      fun solve(input: Int): Int {
        if (input == 99) return input
        return input + calls++
      }
    """.trimIndent()

    val program = KotlinLanguageExecutionConfig().prepare(
      source,
      "fun main() = println(solve(System.`in`.bufferedReader().readText().toInt()))",
    )
    val result = CodeGrader(executionService()).gradeCode(
      "test-123", IMAGE, program,
      listOf(case("1", "1"), case("1", "1"), case("99", "99")), 1_000, 128,
    )

    assertEquals(GradingOutcome.WRONG_ANSWER, result.outcome)
    assertEquals(2, result.passedCases)
    assertEquals(
      listOf(TestCaseOutcome.PASSED, TestCaseOutcome.WRONG_ANSWER, TestCaseOutcome.PASSED),
      result.caseResults.map { it.outcome },
    )

    assertExecutionCleanedUp()
  }

  @Test
  fun `failed programs retain both output streams`() {
    val source = """
      fun solve() {
        print("123")
        System.err.print("diagnostic")
        error("failed")
      }
    """.trimIndent()

    val result = runKotlinTests(source, listOf(JsonNull), driver = "fun main() = solve()")

    assertEquals(ProgramStatus.SUCCEEDED, result.status)
    assertEquals(ProgramStatus.FAILED, result.caseResults.single().status)
    assertEquals("123", result.caseResults.single().stdout)
    assertTrue(result.caseResults.single().stderr.startsWith("diagnostic"))
    assertTrue(result.caseResults.single().stderr.contains("IllegalStateException"))

    assertExecutionCleanedUp()
  }

  @Test
  fun `each case receives its own UTF-8 input and EOF in the same JVM`() {
    val source = "fun solve(input: String) = input"
    val driver = "fun main(args: Array<String>) = print(solve(System.`in`.bufferedReader().readText()))"
    val input = JsonPrimitive("héllo 世界")

    val result = runKotlinTests(source, listOf(input, Json.parseToJsonElement("[1,2]")), driver = driver)

    assertEquals(ProgramStatus.SUCCEEDED, result.status)
    assertEquals(listOf(input, Json.parseToJsonElement("[1,2]")), result.caseResults.map { it.outputJson })

    assertExecutionCleanedUp()
  }

  @Test
  fun `exact per-stream byte allowances are retained in each case report`() {
    val source = """
      fun solve() {
        print('"' + "0".repeat(19_998) + '"')
        System.err.print("0".repeat(20_000))
      }
    """.trimIndent()

    val result = runKotlinTests(source, listOf(JsonNull), driver = "fun main() = solve()")

    assertEquals(ProgramStatus.SUCCEEDED, result.caseResults.single().status)
    assertEquals("\"" + "0".repeat(19_998) + "\"", result.caseResults.single().stdout)
    assertEquals("0".repeat(20_000), result.caseResults.single().stderr)

    assertExecutionCleanedUp()
  }

  @Test
  fun `ordinary exceptions and invalid answers retain later outputs and numeric types`() {
    val source = """
      fun solve(input: Int): String = when (input) {
        0 -> error("failed invocation")
        1 -> "1 2"
        else -> "1.0"
      }
    """.trimIndent()

    val result = runKotlinTests(source, listOf(0, 1, 2).map(::JsonPrimitive))

    assertEquals(ProgramStatus.SUCCEEDED, result.status)
    assertEquals(
      listOf(ProgramStatus.FAILED, ProgramStatus.SUCCEEDED, ProgramStatus.SUCCEEDED),
      result.caseResults.map { it.status },
    )
    assertNull(result.caseResults[0].outputJson)
    assertNull(result.caseResults[1].outputJson)
    assertEquals("1 2\n", result.caseResults[1].stdout)
    assertEquals(Json.parseToJsonElement("1.0"), result.caseResults[2].outputJson)

    assertExecutionCleanedUp()
  }

  @Test
  fun `escaped outputs from every case fit the report without changing the case output caps`() {
    val source = """
      fun solve() {
        print(0.toChar().toString().repeat(20_000))
        System.err.print(0.toChar().toString().repeat(20_000))
      }
    """.trimIndent()

    val result = runKotlinTests(source, List(3) { JsonNull }, driver = "fun main() = solve()")

    assertEquals(ProgramStatus.SUCCEEDED, result.status)
    assertEquals(3, result.caseResults.size)
    for (caseResult in result.caseResults) {
      assertNull(caseResult.outputJson)
      assertEquals("\u0000".repeat(20_000), caseResult.stdout)
      assertEquals("\u0000".repeat(20_000), caseResult.stderr)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `compilation failure returns without running and removes its workspace`() {
    val program = PreparedProgram(
      sourceFiles = emptyMap(),
      compileCommand = listOf("sh", "-c", "echo compile-error >&2; exit 1"),
      runCommand = listOf("sleep", "30"),
    )

    val result = executionService().executeCode("compile-failure", IMAGE, program, listOf(JsonNull), 1_000, 128)

    assertEquals(CodeExecutionResult.CompilationFailed, result)
    assertExecutionCleanedUp()
  }

  @Test
  fun `invalid test inputs are rejected before creating an execution workspace`() {
    val program = PreparedProgram(
      sourceFiles = mapOf("source.txt" to "program"),
      compileCommand = listOf("sleep", "30"),
      runCommand = listOf("sleep", "30"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      executionService().executeCode("invalid-inputs", IMAGE, program, emptyList(), 1_000, 128)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `configuring a sandbox does not allocate execution resources`() {
    val program = PreparedProgram(mapOf("input.kt" to "source"), null, listOf("true"))

    executionService().createSandbox("test-123", IMAGE, program)

    assertExecutionCleanedUp()
  }

  @Test
  fun `staging failure removes the partially written workspace`() {
    val program = PreparedProgram(
      sourceFiles = mapOf("Solution.kt" to "source", "../escaped.kt" to "must not be written"),
      compileCommand = listOf("false"),
      runCommand = listOf("true"),
    )

    val failure = assertThrows(IllegalArgumentException::class.java) {
      executionService().executeCode("staging-failure", IMAGE, program, listOf(JsonNull), 1_000, 128)
    }

    assertEquals("Program sources must use plain file names", failure.message)
    assertFalse(Files.exists(executionRoot.resolve("escaped.kt")))
    assertExecutionCleanedUp()
  }

  @Test
  fun `threads left running cannot turn an unfinished JVM into a passing submission`() {
    val source = """
      fun solve(input: Int): Int {
        if (input == 0) Thread { Thread.sleep(30_000) }.start()
        return input
      }
    """.trimIndent()

    val result = runKotlinTests(source, listOf(0, 1).map(::JsonPrimitive))

    assertEquals(ProgramStatus.TIME_LIMIT_EXCEEDED, result.status)

    assertExecutionCleanedUp()
  }

  @Test
  fun `containers run without network application credentials or writable source mounts`() {
    val executionService = executionService()
    assertTrue(executionService.isAvailable(IMAGE), "Build the Kotlin image described in docs/submission-execution.md")
    val program = PreparedProgram(
      sourceFiles = mapOf("source.txt" to "program"),
      compileCommand = null,
      runCommand = listOf("sh", "-c", """
        test "${'$'}(id -u)" = 65534 || { echo 'Wrong user'; exit 1; }
        test -z "${'$'}DB_PASSWORD" || { echo 'Inherited database credentials'; exit 2; }
        # Linux may expose dormant tunnel devices even in an isolated network namespace.
        for interface in /sys/class/net/*; do
          if test -d "${'$'}interface" && test "${'$'}{interface##*/}" != lo; then
            flags=${'$'}(cat "${'$'}interface/flags")
            test ${'$'}((flags & 1)) = 0 || { echo 'Active external interface'; exit 3; }
          fi
        done
        test -z "${'$'}(awk 'NR > 1' /proc/net/route)" || { echo 'External route'; exit 3; }
        test "${'$'}(cat /sys/fs/cgroup/memory.max)" = 134217728 || { echo 'Wrong memory limit'; exit 4; }
        if touch /workspace/unwanted 2>/dev/null; then echo 'Writable workspace'; exit 5; fi
        if touch /etc/unwanted 2>/dev/null; then echo 'Writable root'; exit 6; fi
        if echo altered > /run/submission-input 2>/dev/null; then echo 'Writable input'; exit 7; fi
        printf '%s\n' '${TestSuiteProtocol.encodeCaseStarted(0)}'
        printf '%s\n' '${TestSuiteProtocol.encodeCaseFinished(0, ProgramResult(ProgramStatus.SUCCEEDED, "null"))}'
        printf '%s\n' '${TestSuiteProtocol.encodeSuiteFinished(ProgramStatus.SUCCEEDED)}'
      """.trimIndent()),
    )

    val execution = executionService.executeCode("test-123", IMAGE, program, listOf(JsonNull), 2_000, 128)
    val result = assertInstanceOf(CodeExecutionResult.Completed::class.java, execution)

    assertEquals(ProgramStatus.SUCCEEDED, result.status, result.caseResults.single().stdout + result.caseResults.single().stderr)
    assertNotNull(result.runtimeMs)

    assertExecutionCleanedUp()
  }

  @ParameterizedTest(name = "mode {0}: {1}")
  @CsvSource(
    "1, TIME_LIMIT_EXCEEDED",
    "2, OUTPUT_LIMIT_EXCEEDED",
    "3, OUTPUT_LIMIT_EXCEEDED",
    "4, MEMORY_LIMIT_EXCEEDED",
    "5, FAILED",
    "6, FAILED",
  )
  fun `timeouts output floods crashes and heap exhaustion identify the active case`(mode: Int, expected: ProgramStatus) {
    val source = """
      fun solve(mode: Int): Int {
        when (mode) {
          1 -> while (true) {}
          2 -> print("x".repeat(20_001))
          3 -> System.err.print("x".repeat(20_001))
          4 -> {
            val values = mutableListOf<ByteArray>()
            while (true) values.add(ByteArray(1_000_000))
          }
          5 -> kotlin.system.exitProcess(0)
          6 -> kotlin.system.exitProcess(124)
        }
        return mode
      }
    """.trimIndent()

    val result = runKotlinTests(source, listOf(0, mode).map(::JsonPrimitive))

    assertEquals(2, result.caseResults.size)
    assertEquals(JsonPrimitive(0), result.caseResults.first().outputJson)
    val failed = result.caseResults.last()
    assertEquals(expected, failed.status, "Mode $mode: ${failed.stderr}")
    assertTrue(failed.stdout.toByteArray(Charsets.UTF_8).size <= 20_000)
    assertTrue(failed.stderr.toByteArray(Charsets.UTF_8).size <= 20_000)

    assertExecutionCleanedUp()
  }

  @Test
  fun `multibyte output overflow stays within the byte cap and does not stop later cases`() {
    val source = """
      fun solve(input: Int): Int {
        if (input == 0) print("世".repeat(7_000))
        return input
      }
    """.trimIndent()

    val result = runKotlinTests(source, listOf(0, 1).map(::JsonPrimitive))

    assertEquals(ProgramStatus.SUCCEEDED, result.status)
    assertEquals(ProgramStatus.OUTPUT_LIMIT_EXCEEDED, result.caseResults.first().status)
    assertTrue(result.caseResults.first().stdout.toByteArray(Charsets.UTF_8).size <= 20_000)
    assertEquals(JsonPrimitive(1), result.caseResults.last().outputJson)

    assertExecutionCleanedUp()
  }

  @Test
  fun `restart cleanup removes only execution workspaces`() {
    val executionService = executionService()
    val abandonedWorkspace = Files.createTempDirectory(executionRoot, "submission-")
    Files.writeString(abandonedWorkspace.resolve("input.kt"), "source")
    val unrelated = Files.writeString(executionRoot.resolve("keep.txt"), "unrelated")
    Files.writeString(executionRoot.resolve("submission-input-leftover.txt"), "input")
    val ownedContainer = docker.createContainerCmd(IMAGE)
      .withLabels(mapOf("interview-forge.execution" to ownershipLabel().substringAfter('=')))
      .exec().id
    val unrelatedContainer = docker.createContainerCmd(IMAGE)
      .withLabels(mapOf("interview-forge.execution" to "another-workspace"))
      .exec().id

    try {
      executionService.cleanUpInterruptedExecutions()

      assertThrows(NotFoundException::class.java) { docker.inspectContainerCmd(ownedContainer).exec() }
      assertEquals(unrelatedContainer, docker.inspectContainerCmd(unrelatedContainer).exec().id)
      assertEquals("unrelated", Files.readString(unrelated))
      Files.list(executionRoot).use { assertEquals(listOf(unrelated), it.toList()) }
    } finally {
      for (id in listOf(ownedContainer, unrelatedContainer)) {
        try {
          docker.removeContainerCmd(id).withForce(true).exec()
        } catch (_: NotFoundException) {
          // The cleanup under test already removed the owned container.
        }
      }
    }
  }

  @Test
  fun `interrupting execution removes the running container and workspace`() {
    val label = ownershipLabel()
    val program = PreparedProgram(emptyMap(), null, listOf("sleep", "30"))
    val failure = AtomicReference<Throwable>()
    val thread = Thread.ofVirtual().start {
      try {
        executionService().executeCode("test-123", IMAGE, program, listOf(JsonNull), 30_000, 128)
      } catch (exception: Throwable) {
        failure.set(exception)
      }
    }

    try {
      val deadline = System.nanoTime() + 5_000_000_000
      while (containersFor(label, runningOnly = true).isEmpty() && System.nanoTime() < deadline) {
        Thread.sleep(20)
      }
      assertFalse(containersFor(label, runningOnly = true).isEmpty(), "The test container did not start")

      val running = containersFor(label, runningOnly = true).single()
      assertEquals("test-123", running.labels["interview-forge.execution-id"])

      thread.interrupt()
      thread.join(5_000)

      assertFalse(thread.isAlive)
      assertInstanceOf(InterruptedException::class.java, failure.get(), failure.get()?.stackTraceToString())
      assertTrue(containersFor(label).isEmpty(), "Cancelled execution left a container behind")
      assertExecutionCleanedUp()
    } finally {
      thread.interrupt()
      thread.join(5_000)
    }
  }

  private fun runKotlinTests(
    source: String,
    inputs: List<JsonElement>,
    driver: String = "fun main() = println(solve(System.`in`.bufferedReader().readText().toInt()))",
  ): CodeExecutionResult.Completed {
    val program = KotlinLanguageExecutionConfig().prepare(source, driver)
    val result = executionService().executeCode("test-123", IMAGE, program, inputs, 1_000, 128)

    return assertInstanceOf(CodeExecutionResult.Completed::class.java, result)
  }

  private fun case(input: String, expected: String) = TestCaseInput(
    Json.parseToJsonElement(input),
    Json.parseToJsonElement(expected),
  )

  private fun containersFor(label: String, runningOnly: Boolean = false) = docker.listContainersCmd()
    .withShowAll(!runningOnly)
    .withLabelFilter(listOf(label))
    .exec()

  private fun ownershipLabel() = "interview-forge.execution=" + MessageDigest.getInstance("SHA-256")
    .digest(executionRoot.toAbsolutePath().normalize().toString().toByteArray())
    .joinToString("") { "%02x".format(it) }

  private fun assertExecutionCleanedUp() {
    Files.list(executionRoot).use { assertEquals(0, it.count()) }
    assertTrue(containersFor(ownershipLabel()).isEmpty(), "Execution left a container behind")
  }

  companion object {
    const val IMAGE = "interview-forge-kotlin:2.4.20"
  }
}

package com.application.execution

import com.application.config.ExecutionProperties
import com.application.config.DockerConfiguration
import com.github.dockerjava.api.exception.NotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

@Timeout(30)
class DockerExecutionServiceTest {
  @TempDir
  lateinit var workspace: Path

  private val docker = DockerConfiguration().dockerClient()

  private fun executionService() = DockerExecutionService(
    ExecutionProperties(workspaceDirectory = workspace.toString()),
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

    executionService.prepareProgram(123, missingImage, PreparedProgram(emptyMap(), null, listOf("true"))).use {
      assertThrows(NotFoundException::class.java) { it.runTestSuite(listOf(case("null", "null")), 2_000, 128) }
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

    preparedSubmission(source).use {
      val result = it.runTestSuite(listOf(case("1", "1"), case("1", "2"), case("1", "3")), 1_000, 128)
      assertEquals(TestSuiteStatus.PASSED, result.status, result.stderr)
      assertEquals(3, result.passedCases)
      assertNull(result.failedCaseIndex)
      assertEquals("", result.stdout)
      assertEquals("", result.stderr)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `global state leaking between cases reports the first failure and stops the suite`() {
    val source = """
      var calls = 0
      fun solve(input: Int): Int {
        if (input == 99) while (true) {}
        return input + calls++
      }
    """.trimIndent()

    preparedSubmission(source).use {
      val result = it.runTestSuite(listOf(case("1", "1"), case("1", "1"), case("99", "99")), 1_000, 128)
      assertEquals(TestSuiteStatus.WRONG_ANSWER, result.status, result.stderr)
      assertEquals(1, result.passedCases)
      assertEquals(1, result.failedCaseIndex)
      assertEquals("2\n", result.stdout)
    }

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

    preparedSubmission(source, "fun main() = solve()").use {
      val result = it.runTestSuite(listOf(case("null", "null")), 1_000, 128)
      assertEquals(TestSuiteStatus.RUNTIME_ERROR, result.status)
      assertEquals(0, result.failedCaseIndex)
      assertEquals("123", result.stdout)
      assertTrue(result.stderr.startsWith("diagnostic"))
      assertTrue(result.stderr.contains("IllegalStateException"))
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `each case receives its own UTF-8 input and EOF in the same JVM`() {
    val source = "fun solve(input: String) = input"
    val driver = "fun main(args: Array<String>) = print(solve(System.`in`.bufferedReader().readText()))"
    val input = JsonPrimitive("héllo 世界")

    preparedSubmission(source, driver).use {
      val result = it.runTestSuite(listOf(TestCaseInput(input, input), case("[1,2]", "[1,2]")), 1_000, 128)
      assertEquals(TestSuiteStatus.PASSED, result.status, result.stderr)
      assertEquals(2, result.passedCases)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `exact per-stream byte allowances are retained in the failed-case report`() {
    val source = """
      fun solve() {
        print('"' + "0".repeat(19_998) + '"')
        System.err.print("0".repeat(20_000))
      }
    """.trimIndent()

    preparedSubmission(source, "fun main() = solve()").use {
      val result = it.runTestSuite(listOf(case("null", "null")), 1_000, 128)
      assertEquals(TestSuiteStatus.WRONG_ANSWER, result.status)
      assertEquals("\"" + "0".repeat(19_998) + "\"", result.stdout)
      assertEquals("0".repeat(20_000), result.stderr)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `suite comparisons retain numeric types and reject malformed JSON`() {
    preparedSubmission("fun solve(input: Int): String = if (input == 0) \"1.0\" else \"1 2\"").use {
      val mismatch = it.runTestSuite(listOf(case("0", "1")), 1_000, 128)
      assertEquals(TestSuiteStatus.WRONG_ANSWER, mismatch.status)

      val malformed = it.runTestSuite(listOf(case("1", "1")), 1_000, 128)
      assertEquals(TestSuiteStatus.INVALID_OUTPUT, malformed.status)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `escaped failure output fits the report without changing the case output caps`() {
    val source = """
      fun solve() {
        print(0.toChar().toString().repeat(20_000))
        System.err.print(0.toChar().toString().repeat(20_000))
      }
    """.trimIndent()

    preparedSubmission(source, "fun main() = solve()").use {
      val result = it.runTestSuite(listOf(case("null", "null")), 1_000, 128)
      assertEquals(TestSuiteStatus.INVALID_OUTPUT, result.status)
      assertEquals("\u0000".repeat(20_000), result.stdout)
      assertEquals("\u0000".repeat(20_000), result.stderr)
    }

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

    preparedSubmission(source).use {
      val result = it.runTestSuite(listOf(case("0", "0"), case("1", "1")), 1_000, 128)
      assertEquals(TestSuiteStatus.TIME_LIMIT_EXCEEDED, result.status)
    }

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
        printf '%s\n' '${TestSuiteProtocol.encodeResult(TestSuiteResult(TestSuiteStatus.PASSED, 1))}'
      """.trimIndent()),
    )

    executionService.prepareProgram(123, IMAGE, program).use {
      val result = it.runTestSuite(listOf(case("null", "null")), 2_000, 128)
      assertEquals(TestSuiteStatus.PASSED, result.status, result.stdout + result.stderr)
      assertNotNull(result.runtimeMs)
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `timeouts output floods crashes and heap exhaustion identify the active case`() {
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

    preparedSubmission(source).use { submission ->
      for ((mode, expected) in listOf(
        1 to TestSuiteStatus.TIME_LIMIT_EXCEEDED,
        2 to TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED,
        3 to TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED,
        4 to TestSuiteStatus.MEMORY_LIMIT_EXCEEDED,
        5 to TestSuiteStatus.RUNTIME_ERROR,
        6 to TestSuiteStatus.RUNTIME_ERROR,
      )) {
        val result = submission.runTestSuite(listOf(case("0", "0"), case(mode.toString(), "0")), 1_000, 128)
        assertEquals(expected, result.status, "Mode $mode: ${result.stderr}")
        assertEquals(1, result.passedCases)
        assertEquals(1, result.failedCaseIndex)
        assertTrue(result.stdout.length <= 20_000)
        assertTrue(result.stderr.length <= 20_000)
        assertTrue(containersFor(workspaceLabel()).isEmpty())
      }
    }

    assertExecutionCleanedUp()
  }

  @Test
  fun `restart cleanup removes only execution workspaces`() {
    val executionService = executionService()
    executionService.prepareProgram(123, IMAGE, PreparedProgram(mapOf("input.kt" to "source"), null, listOf("true")))
    val unrelated = Files.writeString(workspace.resolve("keep.txt"), "unrelated")
    Files.writeString(workspace.resolve("submission-input-leftover.txt"), "input")
    val ownedContainer = docker.createContainerCmd(IMAGE)
      .withLabels(mapOf("interview-forge.execution" to workspaceLabel().substringAfter('=')))
      .exec().id
    val unrelatedContainer = docker.createContainerCmd(IMAGE)
      .withLabels(mapOf("interview-forge.execution" to "another-workspace"))
      .exec().id

    try {
      executionService.cleanUpInterruptedExecutions()

      assertThrows(NotFoundException::class.java) { docker.inspectContainerCmd(ownedContainer).exec() }
      assertEquals(unrelatedContainer, docker.inspectContainerCmd(unrelatedContainer).exec().id)
      assertEquals("unrelated", Files.readString(unrelated))
      Files.list(workspace).use { assertEquals(listOf(unrelated), it.toList()) }
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
    val label = workspaceLabel()
    val program = executionService().prepareProgram(
      123, IMAGE,
      PreparedProgram(emptyMap(), null, listOf("sleep", "30")),
    )
    val failure = AtomicReference<Throwable>()
    val thread = Thread.ofVirtual().start {
      try {
        program.use { it.runTestSuite(listOf(case("null", "null")), 30_000, 128) }
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
      assertEquals("123", running.labels["interview-forge.submission"])

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

  private fun preparedSubmission(
    source: String,
    driver: String = "fun main() = println(solve(System.`in`.bufferedReader().readText().toInt()))",
  ): ProgramExecution {
    val program = KotlinLanguageExecutionConfig().prepare(source, driver)
    val submission = executionService().prepareProgram(123, IMAGE, program)

    try {
      val compiled = submission.compileProgram()
      assertEquals(ProgramStatus.SUCCEEDED, compiled.status, compiled.stderr)
      return submission
    } catch (failure: Throwable) {
      submission.close()
      throw failure
    }
  }

  private fun case(input: String, expected: String) = TestCaseInput(
    Json.parseToJsonElement(input),
    Json.parseToJsonElement(expected),
  )

  private fun containersFor(label: String, runningOnly: Boolean = false) = docker.listContainersCmd()
    .withShowAll(!runningOnly)
    .withLabelFilter(listOf(label))
    .exec()

  private fun workspaceLabel() = "interview-forge.execution=" + MessageDigest.getInstance("SHA-256")
    .digest(workspace.toAbsolutePath().normalize().toString().toByteArray())
    .joinToString("") { "%02x".format(it) }

  private fun assertExecutionCleanedUp() {
    Files.list(workspace).use { assertEquals(0, it.count()) }
    assertTrue(containersFor(workspaceLabel()).isEmpty(), "Execution left a container behind")
  }

  companion object {
    const val IMAGE = "interview-forge-kotlin:2.4.20"
  }
}

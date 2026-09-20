package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CodeGraderTest {
  private val executionService = FakeCodeExecutionService()
  private val grader = CodeGrader(executionService)
  private val program = PreparedProgram(
    sourceFiles = mapOf("Solution.kt" to "submitted source", "TestDriver.kt" to "private driver"),
    compileCommand = listOf("compiler", "Solution.kt", "TestDriver.kt"),
    runCommand = listOf("run-program"),
  )

  @Test
  fun `executes the complete input list once and keeps expected answers in the grader`() {
    val cases = listOf(
      testCase("1", "11"),
      testCase("2", "12"),
      testCase("null", "null"),
    )
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.SUCCEEDED,
      caseResults = listOf(
        caseResult("1", "11", runtimeMs = 10),
        caseResult("2", "12", runtimeMs = 12),
        caseResult("null", runtimeMs = 18),
      ),
      runtimeMs = 42,
    )

    val result = grade(cases)

    assertEquals(1, executionService.calls)
    assertEquals("grading-job-7", executionService.executionId)
    assertEquals("test-runtime", executionService.runtime)
    assertSame(program, executionService.program)
    assertEquals(cases.map { it.input }, executionService.inputs)
    assertEquals(1000, executionService.timeLimitMs)
    assertEquals(256, executionService.memoryLimitMb)
    assertEquals(GradingOutcome.PASSED, result.outcome)
    assertEquals(3, result.passedCases)
    assertEquals(42L, result.runtimeMs)
    val executions = result.caseResults.map { checkNotNull(it.execution) }
    assertEquals(cases.map { it.input }, executions.map { it.inputJson })
    assertEquals(cases.map { it.expectedOutput }, executions.map { it.outputJson })
    assertEquals(listOf(10L, 12L, 18L), executions.map { it.runtimeMs })
  }

  @Test
  fun `grades later cases after wrong answers and invalid JSON`() {
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.SUCCEEDED,
      caseResults = listOf(
        caseResult("1", "0"),
        caseResult("2", output = null, stdout = "not JSON"),
        caseResult("3", stderr = "user diagnostic"),
      ),
    )

    val result = grade(listOf(testCase("1"), testCase("2"), testCase("3")))

    assertEquals(GradingOutcome.WRONG_ANSWER, result.outcome)
    assertEquals(1, result.passedCases)
    assertEquals(
      listOf(TestCaseOutcome.WRONG_ANSWER, TestCaseOutcome.INVALID_OUTPUT, TestCaseOutcome.PASSED),
      result.caseResults.map { it.outcome },
    )
    val executions = result.caseResults.map { checkNotNull(it.execution) }
    assertEquals(listOf("0", "not JSON", "3"), executions.map { it.stdout })
    assertEquals("user diagnostic", executions[2].stderr)
    assertNull(executions[1].outputJson)
  }

  @Test
  fun `comparison preserves JSON null numeric types and large integer precision`() {
    val cases = listOf(testCase("null"), testCase("1"), testCase("9007199254740993"))
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.SUCCEEDED,
      caseResults = listOf(
        caseResult("null"),
        caseResult("1", "1.0"),
        caseResult("9007199254740993", "9007199254740992"),
      ),
    )

    val result = grade(cases)

    assertEquals(GradingOutcome.WRONG_ANSWER, result.outcome)
    assertEquals(1, result.passedCases)
    assertEquals(JsonNull, checkNotNull(result.caseResults[0].execution).outputJson)
    assertEquals(
      listOf(TestCaseOutcome.PASSED, TestCaseOutcome.WRONG_ANSWER, TestCaseOutcome.WRONG_ANSWER),
      result.caseResults.map { it.outcome },
    )
  }

  @Test
  fun `compilation failure leaves every case unrun`() {
    executionService.result = CodeExecutionResult.CompilationFailed

    val result = grade(listOf(testCase("1"), testCase("2")))

    assertEquals(GradingOutcome.COMPILE_ERROR, result.outcome)
    assertEquals(0, result.passedCases)
    assertEquals(2, result.caseResults.size)
    assertTrue(result.caseResults.all { it.outcome == TestCaseOutcome.NOT_RUN })
    assertTrue(result.caseResults.all { it.execution == null })
    assertNull(result.runtimeMs)
  }

  @Test
  fun `execution failures retain their outcomes without comparing their partial output`() {
    for ((status, outcome) in listOf(
      ProgramStatus.FAILED to GradingOutcome.RUNTIME_ERROR,
      ProgramStatus.TIME_LIMIT_EXCEEDED to GradingOutcome.TIME_LIMIT_EXCEEDED,
      ProgramStatus.MEMORY_LIMIT_EXCEEDED to GradingOutcome.MEMORY_LIMIT_EXCEEDED,
      ProgramStatus.OUTPUT_LIMIT_EXCEEDED to GradingOutcome.OUTPUT_LIMIT_EXCEEDED,
    )) {
      executionService.result = CodeExecutionResult.Completed(
        status = status,
        caseResults = listOf(caseResult(
          input = "1",
          output = null,
          status = status,
          stdout = "not JSON",
          stderr = "user diagnostic",
          runtimeMs = 23,
        )),
      )

      val result = grade(listOf(testCase("1")))

      assertEquals(outcome, result.outcome)
      assertEquals(outcome.name, result.caseResults.single().outcome.name)

      val execution = checkNotNull(result.caseResults.single().execution)
      assertEquals(status, execution.status)
      assertEquals("not JSON", execution.stdout)
      assertEquals("user diagnostic", execution.stderr)
      assertNull(execution.outputJson)
      assertEquals(23L, execution.runtimeMs)
      assertEquals(0, result.passedCases)
    }
  }

  @Test
  fun `a failed process exit cannot pass even when every output matched`() {
    for ((status, outcome) in listOf(
      ProgramStatus.FAILED to GradingOutcome.RUNTIME_ERROR,
      ProgramStatus.TIME_LIMIT_EXCEEDED to GradingOutcome.TIME_LIMIT_EXCEEDED,
      ProgramStatus.MEMORY_LIMIT_EXCEEDED to GradingOutcome.MEMORY_LIMIT_EXCEEDED,
      ProgramStatus.OUTPUT_LIMIT_EXCEEDED to GradingOutcome.OUTPUT_LIMIT_EXCEEDED,
    )) {
      executionService.result = CodeExecutionResult.Completed(
        status = status,
        caseResults = listOf(caseResult("1"), caseResult("2")),
        runtimeMs = 5000,
      )

      val result = grade(listOf(testCase("1"), testCase("2")))

      assertEquals(outcome, result.outcome)
      assertEquals(2, result.passedCases)
      assertEquals(5000L, result.runtimeMs)
      assertTrue(result.caseResults.all { it.outcome == TestCaseOutcome.PASSED })
    }
  }

  @Test
  fun `partial execution keeps completed results and marks trailing cases unrun`() {
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.TIME_LIMIT_EXCEEDED,
      caseResults = listOf(
        caseResult("1"),
        caseResult("2", output = null, status = ProgramStatus.TIME_LIMIT_EXCEEDED),
      ),
    )

    val result = grade(listOf(testCase("1"), testCase("2"), testCase("3")))

    assertEquals(GradingOutcome.TIME_LIMIT_EXCEEDED, result.outcome)
    assertEquals(1, result.passedCases)
    assertEquals(
      listOf(TestCaseOutcome.PASSED, TestCaseOutcome.TIME_LIMIT_EXCEEDED, TestCaseOutcome.NOT_RUN),
      result.caseResults.map { it.outcome },
    )
    assertNull(result.caseResults[2].execution)
  }

  @Test
  fun `failure before the first case leaves every case unrun`() {
    executionService.result = CodeExecutionResult.Completed(ProgramStatus.FAILED, emptyList())

    val result = grade(listOf(testCase("1"), testCase("2")))

    assertEquals(GradingOutcome.RUNTIME_ERROR, result.outcome)
    assertEquals(0, result.passedCases)
    assertEquals(2, result.caseResults.size)
    assertTrue(result.caseResults.all { it.outcome == TestCaseOutcome.NOT_RUN })
    assertTrue(result.caseResults.all { it.execution == null })
  }

  @Test
  fun `successful execution requires every output and no execution may return extra outputs`() {
    for (execution in listOf(
      CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, emptyList()),
      CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, listOf(caseResult("1"), caseResult("2"))),
      CodeExecutionResult.Completed(ProgramStatus.FAILED, listOf(caseResult("1"), caseResult("2"))),
    )) {
      executionService.result = execution

      assertThrows(IllegalStateException::class.java) { grade(listOf(testCase("1"))) }
    }
  }

  @Test
  fun `output limits count UTF-8 bytes for JSON answers and both streams`() {
    val atLimit = "\"${"é".repeat(9999)}\""
    val overLimit = "\"${"é".repeat(10_000)}\""
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.SUCCEEDED,
      caseResults = listOf(
        caseResult(atLimit),
        caseResult(overLimit),
        caseResult("1", stderr = "é".repeat(10_001)),
        caseResult("4", output = overLimit, stdout = ""),
      ),
    )

    val result = grade(listOf(testCase(atLimit), testCase(overLimit), testCase("1"), testCase("4", overLimit)))

    assertEquals(GradingOutcome.OUTPUT_LIMIT_EXCEEDED, result.outcome)
    assertEquals(1, result.passedCases)
    assertEquals(
      listOf(
        TestCaseOutcome.PASSED,
        TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED,
        TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED,
        TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED,
      ),
      result.caseResults.map { it.outcome },
    )
  }

  @Test
  fun `grades the structured JSON answer separately from diagnostic streams`() {
    val execution = caseResult(
      input = "1",
      output = "2",
      stdout = "debug: input received",
      stderr = "user diagnostic",
      runtimeMs = 7,
    )
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.SUCCEEDED,
      caseResults = listOf(execution),
    )

    val result = grade(listOf(testCase("1", "2")))
    val testResult = result.caseResults.single()

    assertEquals(GradingOutcome.PASSED, result.outcome)
    assertSame(execution, testResult.execution)
  }

  @Test
  fun `rejects case results that do not match the requested input order`() {
    executionService.result = CodeExecutionResult.Completed(
      status = ProgramStatus.SUCCEEDED,
      caseResults = listOf(caseResult("2"), caseResult("1")),
    )

    assertThrows(IllegalStateException::class.java) { grade(listOf(testCase("1"), testCase("2"))) }
  }

  @Test
  fun `unexpected execution failures and interruption propagate to the caller`() {
    for (failure in listOf(IllegalStateException("execution unavailable"), InterruptedException("cancelled"))) {
      executionService.failure = failure

      val thrown = assertThrows(failure.javaClass) { grade(listOf(testCase("1"))) }

      assertSame(failure, thrown)
    }
  }

  @Test
  fun `empty cases are rejected before execution starts`() {
    assertThrows(IllegalArgumentException::class.java) { grade(emptyList()) }

    assertEquals(0, executionService.calls)
  }

  private fun grade(cases: List<TestCaseInput>): GradingResult = grader.gradeCode(
    executionId = "grading-job-7",
    runtime = "test-runtime",
    program = program,
    cases = cases,
    timeLimitMs = 1000,
    memoryLimitMb = 256,
  )

  private fun testCase(input: String, expected: String = input): TestCaseInput = TestCaseInput(
    Json.parseToJsonElement(input),
    Json.parseToJsonElement(expected),
  )

  private fun caseResult(
    input: String,
    output: String? = input,
    status: ProgramStatus = ProgramStatus.SUCCEEDED,
    stdout: String = output.orEmpty(),
    stderr: String = "",
    runtimeMs: Long? = null,
  ): TestCaseExecutionResult = TestCaseExecutionResult(
    inputJson = Json.parseToJsonElement(input),
    status = status,
    outputJson = output?.let(Json::parseToJsonElement),
    stdout = stdout,
    stderr = stderr,
    runtimeMs = runtimeMs,
  )

  private class FakeCodeExecutionService : CodeExecutionService {
    var result: CodeExecutionResult = CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, emptyList())
    var failure: Exception? = null
    var calls = 0
    var executionId: String? = null
    var runtime: String? = null
    var program: PreparedProgram? = null
    var inputs: List<JsonElement> = emptyList()
    var timeLimitMs: Int? = null
    var memoryLimitMb: Int? = null

    override fun executeCode(
      executionId: String,
      runtime: String,
      program: PreparedProgram,
      inputs: List<JsonElement>,
      timeLimitMs: Int,
      memoryLimitMb: Int,
    ): CodeExecutionResult {
      calls++
      this.executionId = executionId
      this.runtime = runtime
      this.program = program
      this.inputs = inputs
      this.timeLimitMs = timeLimitMs
      this.memoryLimitMb = memoryLimitMb

      failure?.let { throw it }
      return result
    }
  }
}

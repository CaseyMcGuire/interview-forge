package com.application.execution

import kotlinx.serialization.json.JsonElement

/** Executes code and grades every output; callers decide which results to store or expose. */
class CodeGrader(private val codeExecutionService: CodeExecutionService) {
  private val outputChecker = JsonOutputChecker()

  fun gradeCode(
    executionId: String,
    runtime: String,
    program: PreparedProgram,
    cases: List<TestCaseInput>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
  ): GradingResult {
    require(cases.isNotEmpty()) { "Grading requires at least one test case" }

    val execution = codeExecutionService.executeCode(
      executionId = executionId,
      runtime = runtime,
      program = program,
      inputs = cases.map { it.input },
      timeLimitMs = timeLimitMs,
      memoryLimitMb = memoryLimitMb,
    )

    return when (execution) {
      CodeExecutionResult.CompilationFailed -> GradingResult(
        outcome = GradingOutcome.COMPILE_ERROR,
        caseResults = cases.map { TestCaseGradingResult(TestCaseOutcome.NOT_RUN) },
      )

      is CodeExecutionResult.Completed -> gradeOutputs(cases, execution)
    }
  }

  private fun gradeOutputs(cases: List<TestCaseInput>, execution: CodeExecutionResult.Completed): GradingResult {
    check(execution.caseResults.size <= cases.size) { "Unexpected case result count" }
    if (execution.status == ProgramStatus.SUCCEEDED) {
      check(execution.caseResults.size == cases.size) { "Incomplete case results from successful execution" }
    }

    val caseResults = cases.mapIndexed { index, testCase ->
      gradeTestCase(testCase, execution.caseResults.getOrNull(index))
    }

    // Matching outputs cannot turn a failed process exit (including leaked threads) into success.
    val outcome = if (execution.status != ProgramStatus.SUCCEEDED) {
      failureOutcome(execution.status)
    } else {
      caseResults.firstOrNull { it.outcome != TestCaseOutcome.PASSED }?.outcome ?: TestCaseOutcome.PASSED
    }

    return GradingResult(GradingOutcome.valueOf(outcome.name), caseResults, execution.runtimeMs)
  }

  private fun gradeTestCase(
    testCase: TestCaseInput,
    execution: TestCaseExecutionResult?,
  ): TestCaseGradingResult {
    if (execution == null) {
      return TestCaseGradingResult(TestCaseOutcome.NOT_RUN)
    }

    check(execution.inputJson == testCase.input) { "Case result does not match its requested input" }

    val outcome = checkCaseOutput(testCase.expectedOutput, execution)

    return TestCaseGradingResult(outcome, execution)
  }

  private fun checkCaseOutput(expected: JsonElement, execution: TestCaseExecutionResult): TestCaseOutcome {
    if (execution.status != ProgramStatus.SUCCEEDED) {
      return failureOutcome(execution.status)
    }

    if (exceedsOutputLimit(execution.stdout) || exceedsOutputLimit(execution.stderr)) {
      return TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED
    }

    val outputJson = execution.outputJson ?: return TestCaseOutcome.INVALID_OUTPUT
    if (exceedsOutputLimit(outputJson.toString())) {
      return TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED
    }

    return when (outputChecker.checkOutput(expected, outputJson.toString())) {
      JsonOutputCheckResult.MATCH -> TestCaseOutcome.PASSED
      JsonOutputCheckResult.MISMATCH -> TestCaseOutcome.WRONG_ANSWER
      JsonOutputCheckResult.INVALID_OUTPUT -> TestCaseOutcome.INVALID_OUTPUT
    }
  }

  private fun exceedsOutputLimit(output: String): Boolean =
    output.toByteArray(Charsets.UTF_8).size > TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES

  private fun failureOutcome(status: ProgramStatus): TestCaseOutcome = when (status) {
    ProgramStatus.FAILED -> TestCaseOutcome.RUNTIME_ERROR
    ProgramStatus.TIME_LIMIT_EXCEEDED -> TestCaseOutcome.TIME_LIMIT_EXCEEDED
    ProgramStatus.MEMORY_LIMIT_EXCEEDED -> TestCaseOutcome.MEMORY_LIMIT_EXCEEDED
    ProgramStatus.OUTPUT_LIMIT_EXCEEDED -> TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED
    ProgramStatus.SUCCEEDED -> error("Successful execution does not have a failure outcome")
  }
}

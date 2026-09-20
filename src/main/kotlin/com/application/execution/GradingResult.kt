package com.application.execution

/** One result per supplied case, in input order, independent of the attempt's storage model. */
class GradingResult(
  val outcome: GradingOutcome,
  val caseResults: List<TestCaseGradingResult>,
  val runtimeMs: Long? = null,
) {
  val passedCases: Int
    get() = caseResults.count { it.outcome == TestCaseOutcome.PASSED }
}

enum class GradingOutcome {
  PASSED,
  COMPILE_ERROR,
  WRONG_ANSWER,
  INVALID_OUTPUT,
  RUNTIME_ERROR,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  OUTPUT_LIMIT_EXCEEDED,
  INTERNAL_ERROR,
}

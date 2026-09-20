package com.application.execution

import com.application.schema.CustomTestSuiteRunOutcome

/** Public explanations are selected from outcomes, never copied from private program diagnostics. */
internal fun customTestSuiteRunErrorMessage(outcome: CustomTestSuiteRunOutcome): String? = when (outcome) {
  CustomTestSuiteRunOutcome.PASSED,
  CustomTestSuiteRunOutcome.WRONG_ANSWER -> null
  CustomTestSuiteRunOutcome.COMPILE_ERROR -> "Compilation failed. Check the solution syntax and required function signature."
  CustomTestSuiteRunOutcome.REFERENCE_SOLUTION_FAILED -> "Expected outputs could not be generated for these inputs."
  CustomTestSuiteRunOutcome.INVALID_OUTPUT -> "The solution did not return valid JSON."
  CustomTestSuiteRunOutcome.RUNTIME_ERROR -> "The solution stopped with an error."
  CustomTestSuiteRunOutcome.TIME_LIMIT_EXCEEDED -> "Execution exceeded the time limit."
  CustomTestSuiteRunOutcome.MEMORY_LIMIT_EXCEEDED -> "Execution exceeded the memory limit."
  CustomTestSuiteRunOutcome.OUTPUT_LIMIT_EXCEEDED -> "The solution exceeded the output limit."
  CustomTestSuiteRunOutcome.INTERNAL_ERROR -> "Execution could not complete. Try running the tests again."
}

internal fun customTestCaseErrorMessage(outcome: TestCaseOutcome): String? = when (outcome) {
  TestCaseOutcome.NOT_RUN -> null
  else -> customTestSuiteRunErrorMessage(CustomTestSuiteRunOutcome.valueOf(outcome.name))
}

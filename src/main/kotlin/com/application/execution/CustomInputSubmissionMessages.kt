package com.application.execution

import com.application.schema.CustomInputSubmissionOutcome

/** Public explanations are selected from outcomes, never copied from private program diagnostics. */
internal fun customInputSubmissionErrorMessage(outcome: CustomInputSubmissionOutcome): String? = when (outcome) {
  CustomInputSubmissionOutcome.PASSED,
  CustomInputSubmissionOutcome.WRONG_ANSWER -> null
  CustomInputSubmissionOutcome.COMPILE_ERROR -> "Compilation failed. Check the solution syntax and required function signature."
  CustomInputSubmissionOutcome.REFERENCE_SOLUTION_FAILED -> "Expected outputs could not be generated for these inputs."
  CustomInputSubmissionOutcome.INVALID_OUTPUT -> "The solution did not return valid JSON."
  CustomInputSubmissionOutcome.RUNTIME_ERROR -> "The solution stopped with an error."
  CustomInputSubmissionOutcome.TIME_LIMIT_EXCEEDED -> "Execution exceeded the time limit."
  CustomInputSubmissionOutcome.MEMORY_LIMIT_EXCEEDED -> "Execution exceeded the memory limit."
  CustomInputSubmissionOutcome.OUTPUT_LIMIT_EXCEEDED -> "The solution exceeded the output limit."
  CustomInputSubmissionOutcome.INTERNAL_ERROR -> "Execution could not complete. Try running the tests again."
}

internal fun customTestCaseErrorMessage(outcome: TestCaseOutcome): String? = when (outcome) {
  TestCaseOutcome.NOT_RUN -> null
  else -> customInputSubmissionErrorMessage(CustomInputSubmissionOutcome.valueOf(outcome.name))
}

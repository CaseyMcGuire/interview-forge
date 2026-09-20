package com.application.execution

import com.application.schema.SubmissionTestOutcome
import com.application.schema.SubmissionVerdict

internal fun GradingOutcome.toSubmissionVerdict(): SubmissionVerdict = when (this) {
  GradingOutcome.PASSED -> SubmissionVerdict.ACCEPTED
  GradingOutcome.COMPILE_ERROR -> SubmissionVerdict.COMPILE_ERROR
  GradingOutcome.WRONG_ANSWER -> SubmissionVerdict.WRONG_ANSWER
  GradingOutcome.TIME_LIMIT_EXCEEDED -> SubmissionVerdict.TIME_LIMIT_EXCEEDED
  GradingOutcome.MEMORY_LIMIT_EXCEEDED -> SubmissionVerdict.MEMORY_LIMIT_EXCEEDED
  GradingOutcome.INVALID_OUTPUT,
  GradingOutcome.RUNTIME_ERROR,
  GradingOutcome.OUTPUT_LIMIT_EXCEEDED -> SubmissionVerdict.RUNTIME_ERROR
  GradingOutcome.INTERNAL_ERROR -> SubmissionVerdict.INTERNAL_ERROR
}

internal fun TestCaseOutcome.toSubmissionTestOutcome(): SubmissionTestOutcome = when (this) {
  TestCaseOutcome.PASSED -> SubmissionTestOutcome.PASSED
  TestCaseOutcome.WRONG_ANSWER -> SubmissionTestOutcome.WRONG_ANSWER
  TestCaseOutcome.TIME_LIMIT_EXCEEDED -> SubmissionTestOutcome.TIME_LIMIT_EXCEEDED
  TestCaseOutcome.MEMORY_LIMIT_EXCEEDED -> SubmissionTestOutcome.MEMORY_LIMIT_EXCEEDED
  TestCaseOutcome.INVALID_OUTPUT,
  TestCaseOutcome.RUNTIME_ERROR,
  TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED -> SubmissionTestOutcome.RUNTIME_ERROR
  TestCaseOutcome.INTERNAL_ERROR -> SubmissionTestOutcome.INTERNAL_ERROR
  TestCaseOutcome.NOT_RUN -> SubmissionTestOutcome.SKIPPED
}

package com.application.execution

import com.application.schema.ProblemSubmissionTestOutcome
import com.application.schema.ProblemSubmissionVerdict

internal fun GradingOutcome.toProblemSubmissionVerdict(): ProblemSubmissionVerdict = when (this) {
  GradingOutcome.PASSED -> ProblemSubmissionVerdict.ACCEPTED
  GradingOutcome.COMPILE_ERROR -> ProblemSubmissionVerdict.COMPILE_ERROR
  GradingOutcome.WRONG_ANSWER -> ProblemSubmissionVerdict.WRONG_ANSWER
  GradingOutcome.TIME_LIMIT_EXCEEDED -> ProblemSubmissionVerdict.TIME_LIMIT_EXCEEDED
  GradingOutcome.MEMORY_LIMIT_EXCEEDED -> ProblemSubmissionVerdict.MEMORY_LIMIT_EXCEEDED
  GradingOutcome.INVALID_OUTPUT,
  GradingOutcome.RUNTIME_ERROR,
  GradingOutcome.OUTPUT_LIMIT_EXCEEDED -> ProblemSubmissionVerdict.RUNTIME_ERROR
  GradingOutcome.INTERNAL_ERROR -> ProblemSubmissionVerdict.INTERNAL_ERROR
}

internal fun TestCaseOutcome.toProblemSubmissionTestOutcome(): ProblemSubmissionTestOutcome = when (this) {
  TestCaseOutcome.PASSED -> ProblemSubmissionTestOutcome.PASSED
  TestCaseOutcome.WRONG_ANSWER -> ProblemSubmissionTestOutcome.WRONG_ANSWER
  TestCaseOutcome.TIME_LIMIT_EXCEEDED -> ProblemSubmissionTestOutcome.TIME_LIMIT_EXCEEDED
  TestCaseOutcome.MEMORY_LIMIT_EXCEEDED -> ProblemSubmissionTestOutcome.MEMORY_LIMIT_EXCEEDED
  TestCaseOutcome.INVALID_OUTPUT,
  TestCaseOutcome.RUNTIME_ERROR,
  TestCaseOutcome.OUTPUT_LIMIT_EXCEEDED -> ProblemSubmissionTestOutcome.RUNTIME_ERROR
  TestCaseOutcome.INTERNAL_ERROR -> ProblemSubmissionTestOutcome.INTERNAL_ERROR
  TestCaseOutcome.NOT_RUN -> ProblemSubmissionTestOutcome.SKIPPED
}

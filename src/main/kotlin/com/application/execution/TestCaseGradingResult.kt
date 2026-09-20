package com.application.execution

/** Adds a grading outcome to the original execution result without copying its details. */
class TestCaseGradingResult(
  val outcome: TestCaseOutcome,
  /** Absent for NOT_RUN cases, including when compilation failed. */
  val execution: TestCaseExecutionResult? = null,
)

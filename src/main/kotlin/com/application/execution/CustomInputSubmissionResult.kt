package com.application.execution

import com.application.schema.CustomInputSubmissionOutcome

/** A custom result never establishes acceptance against the problem's official tests. */
class CustomInputSubmissionResult(
  val outcome: CustomInputSubmissionOutcome,
  val caseResults: List<CustomTestCaseResult> = emptyList(),
  val runtimeMs: Long? = null,
)

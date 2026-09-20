package com.application.execution

import com.application.schema.CustomTestSuiteRunOutcome

/** A custom result never establishes acceptance against the problem's official tests. */
class CustomTestSuiteRunResult(
  val outcome: CustomTestSuiteRunOutcome,
  val caseResults: List<CustomTestCaseResult> = emptyList(),
  val runtimeMs: Long? = null,
)

package com.application.execution

import com.application.ent.TestCase
import com.application.schema.SubmissionVerdict

class SubmissionExecutionResult(
  val verdict: SubmissionVerdict,
  val passedCases: Int = 0,
  val runtimeMs: Long? = null,
  val failedCase: TestCase? = null,
  val suiteResult: TestSuiteResult? = null,
)

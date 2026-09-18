package com.application.execution

import com.application.ent.TestCase

class SubmissionExecutionSettings(
  val runtime: String,
  val program: PreparedProgram,
  val timeLimitMs: Int,
  val memoryLimitMb: Int,
  val cases: List<TestCase>,
)

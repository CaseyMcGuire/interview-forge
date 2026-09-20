package com.application.execution

class GradingExecutionSettings(
  val runtime: String,
  val program: PreparedProgram,
  val timeLimitMs: Int,
  val memoryLimitMb: Int,
)

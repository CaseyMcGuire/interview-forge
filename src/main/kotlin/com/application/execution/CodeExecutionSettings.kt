package com.application.execution

class CodeExecutionSettings(
  val runtime: String,
  val program: PreparedProgram,
  val timeLimitMs: Int,
  val memoryLimitMb: Int,
)

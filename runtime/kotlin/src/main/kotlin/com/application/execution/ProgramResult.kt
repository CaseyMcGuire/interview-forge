package com.application.execution

/** Captured process or driver-invocation output, before parsing its JSON answer. */
class ProgramResult(
  val status: ProgramStatus,
  val stdout: String = "",
  val stderr: String = "",
  val runtimeMs: Long? = null,
)

enum class ProgramStatus {
  SUCCEEDED,
  FAILED,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  OUTPUT_LIMIT_EXCEEDED,
}

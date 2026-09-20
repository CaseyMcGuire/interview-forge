package com.application.execution

sealed interface CodeExecutionResult {
  /** No user cases ran; compiler diagnostics are not part of the grading result. */
  data object CompilationFailed : CodeExecutionResult

  /**
   * Case results follow input order; missing trailing results indicate cases that were not reached.
   * A successful process must report every case. Process failure remains separate from case outputs,
   * since a program can produce correct answers and still fail to exit or exceed its memory limit.
   */
  class Completed(
    val status: ProgramStatus,
    val caseResults: List<TestCaseExecutionResult>,
    val runtimeMs: Long? = null,
  ) : CodeExecutionResult
}

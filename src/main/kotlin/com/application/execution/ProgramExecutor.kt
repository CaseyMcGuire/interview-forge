package com.application.execution

/** Prepares isolated programs and checks the availability of their configured runtime. */
interface ProgramExecutor : RuntimeAvailability {
  fun prepareProgram(submissionId: Long, runtime: String, program: PreparedProgram): ProgramExecution

  /** Called before this single-instance worker starts accepting work after a restart. */
  fun cleanUpInterruptedExecutions()
}

/** One prepared program; closing it releases its workspace and execution resources. */
interface ProgramExecution : AutoCloseable {
  /** Compiles once before the suite runs; diagnostics are private to execution. */
  fun compileProgram(): ProgramResult

  /**
   * Runs ordered cases together, returning all passed or the first failure.
   * Time is limited per case; memory is shared across the suite.
   */
  fun runTestSuite(cases: List<TestCaseInput>, timeLimitMs: Int, memoryLimitMb: Int): TestSuiteResult
}

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

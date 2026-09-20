package com.application.execution

import kotlinx.serialization.json.JsonElement

/** Executes prepared code without comparing its outputs or accessing stored attempts. */
interface CodeExecutionService {
  /**
   * Compiles once and runs the ordered inputs in one process, preserving state between cases.
   * Reports each reachable case's input, parsed JSON answer, streams, and measured runtime.
   * Preserves JSON numeric types and precision; invalid answers have no parsed output.
   * Applies a per-case time limit and shared memory limit.
   * Releases execution resources before returning or throwing, including on interruption.
   */
  fun executeCode(
    executionId: String,
    runtime: String,
    program: PreparedProgram,
    inputs: List<JsonElement>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
  ): CodeExecutionResult
}

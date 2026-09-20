package com.application.execution

import kotlinx.serialization.json.JsonElement

/** One executed case, before comparing its answer with the expected output. */
class TestCaseExecutionResult(
  val inputJson: JsonElement,
  val status: ProgramStatus,
  /** Parsed answer from a successful case; null means absent or invalid, while JsonNull is a valid answer. */
  val outputJson: JsonElement? = null,
  val stdout: String = "",
  val stderr: String = "",
  /** Elapsed time for this case, when measured; separate from the complete suite's duration. */
  val runtimeMs: Long? = null,
)

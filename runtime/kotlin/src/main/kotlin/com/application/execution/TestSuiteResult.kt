package com.application.execution

import kotlinx.serialization.json.JsonElement

/** One ordered case and the expected value used by the suite's checker. */
class TestCaseInput(val input: JsonElement, val expectedOutput: JsonElement)

/** Retains only the first failed case's output. Indices are zero-based in the supplied suite. */
class TestSuiteResult(
  val status: TestSuiteStatus,
  val passedCases: Int,
  val failedCaseIndex: Int? = null,
  val stdout: String = "",
  val stderr: String = "",
  /** Duration of the entire suite, not the individual failed case. */
  val runtimeMs: Long? = null,
)

enum class TestSuiteStatus {
  PASSED,
  WRONG_ANSWER,
  INVALID_OUTPUT,
  RUNTIME_ERROR,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  OUTPUT_LIMIT_EXCEEDED,
}

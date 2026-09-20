package com.application.schema

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** One ordered input and expected answer selected for a grading job. */
@Serializable
data class GradingCase(
  val testCaseId: Long,
  val inputJson: JsonElement,
  val expectedOutputJson: JsonElement,
  /** Official-case visibility at enqueue time; absent for custom cases. */
  val visibility: TestCaseVisibility? = null,
)

package com.application.execution

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** Only user-program output is retained; reference diagnostics and stderr never enter this payload. */
data class CustomTestCaseResult(
  val testCaseId: Long,
  val outcome: TestCaseOutcome,
  val output: String = "",
) {
  fun toJson(): JsonObject = buildJsonObject {
    put("testCaseId", testCaseId)
    put("outcome", outcome.name)
    put("output", output)
  }

  companion object {
    fun fromJson(value: JsonObject): CustomTestCaseResult = CustomTestCaseResult(
      testCaseId = value.getValue("testCaseId").jsonPrimitive.long,
      outcome = TestCaseOutcome.valueOf(value.getValue("outcome").jsonPrimitive.content),
      output = value.getValue("output").jsonPrimitive.content,
    )
  }
}

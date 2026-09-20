package com.application.execution

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** Inputs enter the JVM; ordered case outputs and a terminal process status come back. */
object TestSuiteProtocol {
  const val MAX_CASE_OUTPUT_BYTES = 20_000
  // Two bounded streams can each expand sixfold when escaped as JSON.
  const val MAX_CASE_RESULT_BYTES = 256_000

  fun encodeInput(inputs: List<JsonElement>, timeLimitMs: Int): String = buildJsonObject {
    put("timeLimitMs", timeLimitMs)
    put("inputs", JsonArray(inputs))
  }.toString()

  fun decodeInputs(input: JsonObject): List<JsonElement> = input.getValue("inputs").jsonArray

  fun encodeCaseStarted(index: Int): String = buildJsonObject {
    put("event", "caseStarted")
    put("caseIndex", index)
  }.toString()

  fun encodeCaseFinished(index: Int, result: ProgramResult): String = buildJsonObject {
    put("event", "caseFinished")
    put("caseIndex", index)
    put("status", result.status.name)
    put("stdout", result.stdout)
    put("stderr", result.stderr)
    result.runtimeMs?.let { put("runtimeMs", it) }
  }.toString()

  fun encodeSuiteFinished(status: ProgramStatus): String = buildJsonObject {
    put("event", "suiteFinished")
    put("status", status.name)
  }.toString()

  fun decodeCaseOutput(event: JsonObject): ProgramResult = ProgramResult(
    status = ProgramStatus.valueOf(event.getValue("status").jsonPrimitive.content),
    stdout = event.getValue("stdout").jsonPrimitive.content,
    stderr = event.getValue("stderr").jsonPrimitive.content,
    runtimeMs = event["runtimeMs"]?.jsonPrimitive?.long,
  )
}

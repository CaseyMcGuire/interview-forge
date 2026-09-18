package com.application.execution

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** JSON suite input, case-start checkpoints, and one terminal result shared with the application. */
object TestSuiteProtocol {
  const val MAX_CASE_OUTPUT_BYTES = 20_000
  const val MAX_RESULT_BYTES = 256_000

  fun encodeInput(cases: List<TestCaseInput>, timeLimitMs: Int): String = buildJsonObject {
    put("timeLimitMs", timeLimitMs)
    put("cases", JsonArray(cases.map { case ->
      buildJsonObject {
        put("input", case.input)
        put("expectedOutput", case.expectedOutput)
      }
    }))
  }.toString()

  fun decodeCases(input: JsonObject): List<TestCaseInput> = input.getValue("cases").jsonArray.map { case ->
    TestCaseInput(case.jsonObject.getValue("input"), case.jsonObject.getValue("expectedOutput"))
  }

  fun encodeCaseStarted(index: Int): String = buildJsonObject { put("caseIndex", index) }.toString()

  fun encodeResult(result: TestSuiteResult): String = buildJsonObject {
    put("status", result.status.name)
    put("passedCases", result.passedCases)
    result.failedCaseIndex?.let { put("failedCaseIndex", it) }
    put("stdout", result.stdout)
    put("stderr", result.stderr)
  }.toString()

  fun decodeResult(result: JsonObject): TestSuiteResult = TestSuiteResult(
    status = TestSuiteStatus.valueOf(result.getValue("status").jsonPrimitive.content),
    passedCases = result.getValue("passedCases").jsonPrimitive.int,
    failedCaseIndex = result["failedCaseIndex"]?.jsonPrimitive?.int,
    stdout = result.getValue("stdout").jsonPrimitive.content,
    stderr = result.getValue("stderr").jsonPrimitive.content,
  )
}

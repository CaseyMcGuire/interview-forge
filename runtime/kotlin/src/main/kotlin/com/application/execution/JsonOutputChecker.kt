package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tools.jackson.core.JacksonException
import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/** Strict JSON equality that preserves numeric precision and distinguishes integers from decimals. */
class JsonOutputChecker {
  private val mapper = JsonMapper.builder(
    JsonFactory.builder()
      .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(20_000).build())
      .build(),
  )
    .enable(
      DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
      DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
      DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
      DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
    )
    .build()

  fun checkOutput(expected: JsonElement, output: String): JsonOutputCheckResult {
    val actual = readOutput(output) ?: return JsonOutputCheckResult.INVALID_OUTPUT
    val expectedValue = mapper.readTree(expected.toString())

    return if (expectedValue == actual) JsonOutputCheckResult.MATCH else JsonOutputCheckResult.MISMATCH
  }

  /** Parses one strict JSON answer while retaining the original numeric representation. */
  fun parseOutput(output: String): JsonElement? {
    readOutput(output) ?: return null
    return Json.parseToJsonElement(output)
  }

  private fun readOutput(output: String): JsonNode? {
    val actual = try {
      mapper.readTree(output)
    } catch (_: JacksonException) {
      return null
    }

    if (actual == null || actual.isMissingNode) {
      return null
    }

    return actual
  }
}

enum class JsonOutputCheckResult {
  MATCH,
  MISMATCH,
  INVALID_OUTPUT,
}

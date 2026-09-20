package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JsonOutputCheckerTest {
  private val checker = JsonOutputChecker()

  @Test
  fun `parsed answers preserve numeric representations and distinguish JSON null from no answer`() {
    for (value in listOf("1", "1.0", "1e0", "123456789012345678901234567890", "1e-20000")) {
      assertEquals(value, checker.parseOutput(" $value\n").toString())
    }

    assertEquals(JsonNull, checker.parseOutput("null"))
  }

  @Test
  fun `numbers retain precision and object key order is ignored`() {
    for ((expected, actual) in listOf(
      "1" to "1",
      "0.00000000000000000001" to "0.00000000000000000001",
      "123456789012345678901234567890" to "123456789012345678901234567890",
      "{\"a\": [1, null], \"b\": true}" to " {\"b\":true, \"a\":[1,null]} \n",
    )) {
      assertEquals(JsonOutputCheckResult.MATCH, checker.checkOutput(Json.parseToJsonElement(expected), actual))
    }
  }

  @Test
  fun `numeric types JSON types array order and decimal differences matter`() {
    for ((expected, actual) in listOf(
      "1" to "1.0",
      "1.0" to "1",
      "1" to "1e0",
      "123456789012345678901234567890" to "123456789012345678901234567890.0",
      "{\"answer\":[1]}" to "{\"answer\":[1.0]}",
      "1" to "\"1\"",
      "[1,2]" to "[2,1]",
      "false" to "null",
      "0.123456789012345678901" to "0.123456789012345678902",
      "1e-20000" to "0",
    )) {
      assertEquals(JsonOutputCheckResult.MISMATCH, checker.checkOutput(Json.parseToJsonElement(expected), actual))
    }
  }

  @Test
  fun `output must be exactly one strict JSON value`() {
    for (output in listOf("", " ", "01", "+1", "NaN", "Infinity", "1 2", "[1,]", "{\"a\":1,\"a\":2}")) {
      assertNull(checker.parseOutput(output), output)
      assertEquals(
        JsonOutputCheckResult.INVALID_OUTPUT,
        checker.checkOutput(Json.parseToJsonElement("null"), output),
        output,
      )
    }
  }
}

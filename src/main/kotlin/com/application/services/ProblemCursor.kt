package com.application.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import java.util.Base64

/** Position in the title/slug ordering, independent of row offsets. */
data class ProblemCursor(val title: String, val slug: String) {
  fun encode(): String {
    val value = buildJsonArray {
      add("problems:v1")
      add(title)
      add(slug)
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.toString().toByteArray(Charsets.UTF_8))
  }

  companion object {
    fun decode(value: String): ProblemCursor {
      val json = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
      val parts = Json.parseToJsonElement(json) as? JsonArray
      require(parts != null && parts.size == 3 && parts.all { it is JsonPrimitive && it.isString }) {
        "Invalid problem cursor"
      }
      val fields = parts.map { (it as JsonPrimitive).content }
      require(fields[0] == "problems:v1" && fields[2].isNotBlank()) { "Invalid problem cursor" }
      return ProblemCursor(title = fields[1], slug = fields[2])
    }
  }
}

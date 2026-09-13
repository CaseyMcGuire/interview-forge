package com.application.graphql

import java.util.Base64
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Creates and parses Relay-style global object ids: base64url("TypeName:databaseId").
 * Exposing these instead of raw database ids keeps client-side caching keyed by a
 * globally unique, opaque identifier.
 */
@Component
class GlobalIdUtil {

  fun toGlobalId(type: KClass<*>, id: Long): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString("${nameOf(type)}:$id".toByteArray())

  fun fromGlobalIdOrNull(globalId: String): Long? {
    val decoded = try {
      String(Base64.getUrlDecoder().decode(globalId))
    } catch (e: IllegalArgumentException) {
      return null
    }

    return decoded.substringAfter(':', missingDelimiterValue = "").toLongOrNull()
  }

  // Check the type as well as the numeric ID: Problem:42 must not be accepted as
  // ProblemExample:42. Re-encoding also verifies that the ID uses our canonical format.
  fun fromGlobalIdOrNull(globalId: String, type: KClass<*>): Long? {
    val id = fromGlobalIdOrNull(globalId) ?: return null
    return id.takeIf { toGlobalId(type, it) == globalId }
  }

  private fun nameOf(type: KClass<*>): String =
    requireNotNull(type.simpleName) { "Cannot create a global id for an anonymous class" }
}

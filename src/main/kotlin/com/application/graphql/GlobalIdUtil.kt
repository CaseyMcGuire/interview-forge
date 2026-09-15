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
    val decoded = decode(globalId) ?: return null
    return decoded.substringAfter(':', missingDelimiterValue = "").toLongOrNull()
  }

  /** Parses a positive database ID only when the decoded GraphQL type matches. */
  fun fromGlobalIdOrNull(globalId: String, type: KClass<*>): Long? {
    val decoded = decode(globalId) ?: return null
    if (decoded.substringBefore(':') != nameOf(type)) {
      return null
    }

    val id = decoded.substringAfter(':', missingDelimiterValue = "").toLongOrNull() ?: return null
    return id.takeIf { it > 0 }
  }

  private fun decode(globalId: String): String? = try {
    String(Base64.getUrlDecoder().decode(globalId))
  } catch (_: IllegalArgumentException) {
    null
  }

  private fun nameOf(type: KClass<*>): String =
    requireNotNull(type.simpleName) { "Cannot create a global id for an anonymous class" }
}

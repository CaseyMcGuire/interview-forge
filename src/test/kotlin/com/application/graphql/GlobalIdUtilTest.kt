package com.application.graphql

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Base64

class GlobalIdUtilTest {

  private val globalIdUtil = GlobalIdUtil()

  private class Post

  @Test
  fun `round-trips an id`() {
    val globalId = globalIdUtil.toGlobalId(Post::class, 42L)
    assertEquals(42L, globalIdUtil.fromGlobalIdOrNull(globalId))
  }

  @Test
  fun `encodes as base64url of TypeName colon id`() {
    val globalId = globalIdUtil.toGlobalId(Post::class, 7L)
    assertEquals("Post:7", String(Base64.getUrlDecoder().decode(globalId)))
  }

  @Test
  fun `returns null for invalid base64`() {
    assertNull(globalIdUtil.fromGlobalIdOrNull("not!!valid@@base64"))
  }

  @Test
  fun `returns null when the decoded value has no id part`() {
    val noColon = Base64.getUrlEncoder().withoutPadding().encodeToString("Post42".toByteArray())
    val nonNumeric = Base64.getUrlEncoder().withoutPadding().encodeToString("Post:abc".toByteArray())
    assertNull(globalIdUtil.fromGlobalIdOrNull(noColon))
    assertNull(globalIdUtil.fromGlobalIdOrNull(nonNumeric))
  }
}

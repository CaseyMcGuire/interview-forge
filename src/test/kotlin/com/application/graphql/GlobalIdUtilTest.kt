package com.application.graphql

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Base64

class GlobalIdUtilTest {

  private val globalIdUtil = GlobalIdUtil()

  private class Post
  private class Problem

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

  @Test
  fun `typed parsing requires a positive ID and the expected type`() {
    val valid = globalIdUtil.toGlobalId(Problem::class, 42L)
    assertEquals(42L, globalIdUtil.fromGlobalIdOrNull(valid, Problem::class))

    val invalid = listOf(
      "Post:42",
      "Problem:0",
      "Problem:-1",
      "Problem",
      "Problem:",
      "Problem:abc",
      "ProblemExtra:42",
      "Problem:42:extra",
      "Problem:9223372036854775808",
    )
    for (value in invalid) {
      val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
      assertNull(globalIdUtil.fromGlobalIdOrNull(encoded, Problem::class), value)
    }
    assertNull(globalIdUtil.fromGlobalIdOrNull("not!!valid@@base64", Problem::class))
  }

  @Test
  fun `typed parsing accepts equivalent ID representations`() {
    val padded = Base64.getUrlEncoder().encodeToString("Problem:42".toByteArray())
    val leadingZero = Base64.getUrlEncoder().withoutPadding().encodeToString("Problem:042".toByteArray())

    for (value in listOf(padded, leadingZero)) {
      assertEquals(42L, globalIdUtil.fromGlobalIdOrNull(value, Problem::class), value)
    }
  }
}

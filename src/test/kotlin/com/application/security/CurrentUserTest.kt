package com.application.security

import com.application.db.models.UserDetailsImpl
import com.application.schema.UserRole
import com.application.services.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class CurrentUserTest {
  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `database failures do not fall back to the role captured at login`() {
    val jdbcClient = mock(JdbcClient::class.java)
    `when`(jdbcClient.sql(anyString())).thenThrow(DataAccessResourceFailureException("Database unavailable"))
    val principal = UserDetailsImpl(User("admin@example.com", "unused", UserRole.ADMIN, 42L))
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
      principal, null, principal.authorities,
    )

    assertThrows(DataAccessResourceFailureException::class.java) {
      CurrentUser(jdbcClient).requireAdmin()
    }
  }

  @Test
  fun `unauthenticated principals cannot trigger a role lookup`() {
    val jdbcClient = mock(JdbcClient::class.java)
    val principal = UserDetailsImpl(User("admin@example.com", "unused", UserRole.ADMIN, 42L))
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.unauthenticated(principal, null)

    assertNull(CurrentUser(jdbcClient).get())
    verifyNoInteractions(jdbcClient)
  }
}

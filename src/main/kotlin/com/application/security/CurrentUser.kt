package com.application.security

import com.application.db.models.UserDetailsImpl
import com.application.schema.UserRole
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.privacy.longIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

/** Uses the authenticated identity, but reads current permissions from the database. */
@Component
class CurrentUser(private val jdbcClient: JdbcClient) {
  fun get(): AuthenticatedUser? {
    val authentication = SecurityContextHolder.getContext().authentication ?: return null
    if (!authentication.isAuthenticated) return null
    val userId = (authentication.principal as? UserDetailsImpl)?.user?.id ?: return null

    // Ent currently loads whole users, including credentials. Read only the permission
    // needed here; keep credential access and its privacy bypass confined to login.
    return jdbcClient.sql("SELECT role FROM users WHERE id = :id")
      .param("id", userId)
      .query { resultSet, _ -> AuthenticatedUser(userId, UserRole.valueOf(resultSet.getString("role"))) }
      .optional()
      .orElse(null)
  }

  fun requireAdmin(): AuthenticatedUser = get()?.takeIf { it.role == UserRole.ADMIN }
    ?: throw AccessDeniedException("Administrator access is required")

  fun isAdmin(context: ViewerContext): Boolean {
    val user = get() ?: return false
    return user.role == UserRole.ADMIN && user.id == context.longIdOrNull()
  }
}

data class AuthenticatedUser(val id: Long, val role: UserRole)

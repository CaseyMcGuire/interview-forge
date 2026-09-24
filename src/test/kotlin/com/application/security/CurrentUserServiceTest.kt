package com.application.security

import com.application.db.models.UserDetailsImpl
import com.application.db.policies.UserPolicy
import com.application.ent.EntClient
import com.application.schema.UserRole
import com.application.services.User
import entkt.postgres.PostgresDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.sql.SQLException
import javax.sql.DataSource

class CurrentUserServiceTest {
  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `database failures do not fall back to the role captured at login`() {
    val dataSource = mock(DataSource::class.java)
    `when`(dataSource.connection).thenThrow(SQLException("Database unavailable"))
    val principal = UserDetailsImpl(User("admin@example.com", "unused", UserRole.ADMIN, 42L))
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
      principal, null, principal.authorities,
    )

    assertThrows(SQLException::class.java) {
      createService(dataSource).requireAdmin()
    }
  }

  @Test
  fun `unauthenticated principals cannot trigger a role lookup`() {
    val dataSource = mock(DataSource::class.java)
    val principal = UserDetailsImpl(User("admin@example.com", "unused", UserRole.ADMIN, 42L))
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.unauthenticated(principal, null)

    assertNull(createService(dataSource).get())
    verifyNoInteractions(dataSource)
  }

  private fun createService(dataSource: DataSource): CurrentUserService {
    val client = EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies { users(UserPolicy()) }
    }
    val beans = StaticListableBeanFactory()
    beans.addBean("entClient", client)

    return CurrentUserService(beans.getBeanProvider(EntClient::class.java))
  }
}

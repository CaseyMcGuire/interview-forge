package com.application

import com.application.dao.UserDao
import com.application.db.models.UserDetailsImpl
import com.application.ent.EntClient
import com.application.exceptions.UserAlreadyExistsException
import com.application.services.UserService
import com.application.schema.UserRole
import com.application.security.AuthenticatedUser
import com.application.security.CurrentUserService
import com.application.security.UserIdPrincipal
import com.application.services.User
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntConstraintViolationException
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID
import com.application.ent.User as EntUser

/**
 * Basic integration test. Boots the full Spring context against a throwaway PostgreSQL container,
 * lets Flyway apply the migrations in src/main/resources/db/migration, and exercises a real UserDao
 * round-trip. This covers the whole data path end to end: Testcontainers -> DataSource autoconfiguration
 * -> Flyway -> EntKt. Also verifies registration policies and Spring Security credential loading.
 *
 * @ServiceConnection points spring.datasource.* at the container automatically, so no manual property
 * wiring is needed. Requires a running Docker daemon.
 */
@Testcontainers
@SpringBootTest
class UserDaoIntegrationTest {

  private val inspectionContext = ViewerContext.privacyBypass_DANGEROUS(
    "Inspect persisted credentials in integration tests"
  )

  @Autowired
  lateinit var userDao: UserDao

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var userService: UserService

  @Autowired
  lateinit var userDetailsService: UserDetailsService

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  lateinit var currentUserService: CurrentUserService

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `current user reflects role changes and account deletion for browser and MCP identities`(browserIdentity: Boolean) {
    val account = entClient.users.create {
      email = "current-user-${UUID.randomUUID()}@example.com"
      hashedPassword = "private-password-hash"
      role = UserRole.ADMIN
    }.saveAndLoad(inspectionContext).getOrThrow()

    val principal = if (browserIdentity) {
      UserDetailsImpl(User(account.email, account.hashedPassword, account.role, account.id))
    } else {
      UserIdPrincipal(account.id)
    }
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
      principal, null, emptyList(),
    )

    assertEquals(AuthenticatedUser(account.id, UserRole.ADMIN), currentUserService.requireAdmin())
    assertTrue(currentUserService.isAdmin(ViewerContext(Viewer.User(account.id))))
    assertFalse(currentUserService.isAdmin(ViewerContext(Viewer.User(account.id + 1))))

    entClient.users.update(account.id) { role = UserRole.USER }.save(inspectionContext).getOrThrow()

    assertEquals(AuthenticatedUser(account.id, UserRole.USER), currentUserService.get())
    assertFalse(currentUserService.isAdmin(ViewerContext(Viewer.User(account.id))))
    assertThrows(AccessDeniedException::class.java) { currentUserService.requireAdmin() }

    entClient.users.deleteById(inspectionContext, account.id).getOrThrow()

    assertNull(currentUserService.get())
    assertThrows(AccessDeniedException::class.java) { currentUserService.requireAdmin() }
  }

  @Test
  fun `persists a user and reads it back`() {
    val email = "integration@example.com"
    assertNull(userDao.findByEmail(email), "user should not exist before it is created")

    val created = userDao.createUser(email, "hashed-password")
    assertEquals(email, created.username)
    assertEquals("hashed-password", created.hashedPassword)

    val found = userDao.findByEmail(email)
    assertNotNull(found)
    assertEquals(email, found!!.username)
    assertEquals("hashed-password", found.hashedPassword)
    assertEquals(UserRole.USER, found.role)
    assertNotNull(found.id)

    // The database still supplies a BIGSERIAL id without changing the original Flyway migration.
    assertTrue(findEntityByEmail(email).id > 0)
  }

  @Test
  fun `database rejects duplicate emails without changing the existing credentials`() {
    val email = "duplicate@example.com"
    userDao.createUser(email, "original-hash")

    assertThrows(EntConstraintViolationException::class.java) {
      userDao.createUser(email, "replacement-hash")
    }
    assertEquals("original-hash", userDao.findByEmail(email)!!.hashedPassword)
  }

  @Test
  fun `registration hashes passwords and supports Spring Security credential loading`() {
    val email = "registration@example.com"
    val password = "test-password"
    val created = userService.createUser(email, password)

    assertTrue(passwordEncoder.matches(password, created.hashedPassword))
    assertEquals(created, userService.getUserByUsername(email)?.copy(id = null))

    val details = userDetailsService.loadUserByUsername(email)
    assertEquals(email, details.username)
    assertTrue(passwordEncoder.matches(password, details.password))
    assertTrue(details.isEnabled)
    assertEquals(listOf("ROLE_USER"), details.authorities.map { it.authority })
    assertThrows(UserAlreadyExistsException::class.java) {
      userService.createUser(email, "another-password")
    }
  }

  @Test
  fun `public registration allows creation without allowing credential updates or deletion`() {
    val anonymous = ViewerContext(Viewer.Anonymous)
    val email = "public-registration@example.com"
    entClient.users.create {
      this.email = email
      hashedPassword = "original-hash"
    }.save(anonymous).getOrThrow()

    val id = findEntityByEmail(email).id
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.users.update(id) {
        hashedPassword = "replacement-hash"
      }.save(anonymous).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.users.deleteById(anonymous, id).getOrThrow()
    }
    assertEquals("original-hash", userDao.findByEmail(email)!!.hashedPassword)
  }

  @Test
  fun `public registration cannot create an administrator`() {
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.users.create {
        email = "forged-admin@example.com"
        hashedPassword = "hash"
        role = UserRole.ADMIN
      }.save(ViewerContext(Viewer.Anonymous)).getOrThrow()
    }
    assertNull(userDao.findByEmail("forged-admin@example.com"))
  }

  @Test
  fun `ordinary viewers cannot load credentials and generated entities redact hashes`() {
    val email = "private@example.com"
    val hash = "private-password-hash"
    userDao.createUser(email, hash)
    val entity = findEntityByEmail(email)

    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
      UserIdPrincipal(entity.id), null, emptyList(),
    )

    for (viewer in listOf(Viewer.Anonymous, Viewer.User(entity.id))) {
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.users.findById(ViewerContext(viewer), entity.id).getOrThrow()
      }
    }

    assertFalse(entity.toString().contains(hash))
  }

  private fun findEntityByEmail(email: String): EntUser =
    entClient.users.query {
      where(EntUser.email eq email)
    }.firstOrNull(inspectionContext).getOrThrow() ?: error("Expected persisted user: $email")

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

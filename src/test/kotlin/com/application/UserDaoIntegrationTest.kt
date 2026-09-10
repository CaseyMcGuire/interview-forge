package com.application

import com.application.dao.UserDao
import com.application.ent.EntClient
import com.application.exceptions.UserAlreadyExistsException
import com.application.services.UserService
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
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
    assertNull(found.role)

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
    assertEquals(created, userService.getUserByUsername(email))

    val details = userDetailsService.loadUserByUsername(email)
    assertEquals(email, details.username)
    assertTrue(passwordEncoder.matches(password, details.password))
    assertTrue(details.isEnabled)
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
  fun `ordinary viewers cannot load credentials and generated entities redact hashes`() {
    val email = "private@example.com"
    val hash = "private-password-hash"
    userDao.createUser(email, hash)
    val entity = findEntityByEmail(email)

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

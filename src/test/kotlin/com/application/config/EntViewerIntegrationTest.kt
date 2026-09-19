package com.application.config

import com.application.db.models.UserDetailsImpl
import com.application.ent.EntClient
import com.application.schema.UserRole
import com.application.services.User
import entkt.runtime.privacy.ViewerContext
import jakarta.servlet.Filter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@Testcontainers
@SpringBootTest
class EntViewerIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and update isolated Ent Viewer test users",
  )

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var applicationContext: WebApplicationContext

  @Autowired
  @Qualifier("springSecurityFilterChain")
  lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc

  @BeforeEach
  fun setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<DefaultMockMvcBuilder>(securityFilter)
      .build()
  }

  @Test
  fun `anonymous visitors must sign in`() {
    mvc.perform(get("/_ent/schema"))
      .andExpect(status().is3xxRedirection)
      .andExpect(redirectedUrl("/login"))
  }

  @Test
  fun `ordinary users cannot access the viewer`() {
    val (_, session) = createUserSession(UserRole.USER)

    mvc.perform(get("/_ent/schema").session(session))
      .andExpect(status().isNotFound)
  }

  @Test
  fun `administrators can inspect generated fields and relationships`() {
    val (_, session) = createUserSession(UserRole.ADMIN)

    val index = mvc.perform(get("/_ent/schema").session(session))
      .andExpect(status().isOk)
      .andReturn().response.contentAsString
    assertTrue(index.contains("/_ent/schema/submissions"))
    assertTrue(index.contains("/_ent/schema/testCases"))

    val detail = mvc.perform(get("/_ent/schema/submissions").session(session))
      .andExpect(status().isOk)
      .andReturn().response.contentAsString
    assertTrue(detail.contains("source_code"))
    assertTrue(detail.contains("failedTestResult"))
    assertTrue(detail.contains("/_ent/schema/submissionFailures"))
    assertTrue(detail.contains("/_ent/schema/problems"))
  }

  @Test
  fun `removing administrator privileges revokes access for an existing session`() {
    val (userId, session) = createUserSession(UserRole.ADMIN)

    mvc.perform(get("/_ent/schema").session(session))
      .andExpect(status().isOk)

    entClient.users.update(userId) {
      role = UserRole.USER
    }.save(fixtureContext).getOrThrow()

    mvc.perform(get("/_ent/schema").session(session))
      .andExpect(status().isNotFound)
  }

  @Test
  fun `viewer administrator access does not bypass credential read policies`() {
    val (userId, session) = createUserSession(UserRole.ADMIN)

    mvc.perform(get("/_ent/entities/users/$userId").session(session))
      .andExpect(status().isNotFound)
  }

  private fun createUserSession(role: UserRole): Pair<Long, MockHttpSession> {
    val user = entClient.users.create {
      email = "ent-viewer-${UUID.randomUUID()}@example.com"
      hashedPassword = "unused"
      this.role = role
    }.saveAndLoad(fixtureContext).getOrThrow()

    val principal = UserDetailsImpl(User(user.email, "unused", role, user.id))
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = UsernamePasswordAuthenticationToken.authenticated(
      principal, null, principal.authorities,
    )

    val session = MockHttpSession()
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context)
    return user.id to session
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

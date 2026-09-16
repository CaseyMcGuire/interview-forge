package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.JudgeConfiguration
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.schema.ProblemCheckerKind
import com.application.schema.ProblemDifficulty
import com.application.schema.UserRole
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import com.application.graphql.types.JudgeConfiguration as GraphqlJudgeConfiguration
import com.application.graphql.types.Problem as GraphqlProblem
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage

@Testcontainers
@SpringBootTest
class JudgeConfigurationIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect judge configuration fixtures in an isolated test database"
  )

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var applicationContext: WebApplicationContext
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var objectMapper: ObjectMapper
  @Autowired lateinit var globalIdUtil: GlobalIdUtil
  @Autowired
  @Qualifier("springSecurityFilterChain")
  lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc
  private var adminId: Long = 0
  private lateinit var adminSession: MockHttpSession
  private lateinit var problem: Problem
  private lateinit var configuration: ProblemLanguage
  private lateinit var otherConfiguration: ProblemLanguage

  private val problemId get() = globalIdUtil.toGlobalId(GraphqlProblem::class, problem.id)
  private val configurationId get() = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configuration.id)
  private val otherConfigurationId get() = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, otherConfiguration.id)

  private val validSettings = mapOf(
    "testDriverCode" to "fun main() = judge()",
    "timeLimitMs" to 1000,
    "memoryLimitMb" to 256,
  )

  @BeforeEach
  fun setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()

    val login = login(UserRole.ADMIN)
    adminId = login.first
    adminSession = login.second
    problem = entClient.problems.create {
      slug = "judge-${UUID.randomUUID()}"
      title = "Two Sum"
      statementMarkdown = "Find two distinct indices."
      difficulty = ProblemDifficulty.EASY
      createdByUserId = adminId
      publishedAt = Instant.now()
    }.saveAndLoad(fixtureContext).getOrThrow()

    configuration = createConfiguration(createLanguage())
    otherConfiguration = createConfiguration(createLanguage())
  }

  @Test
  fun `creating a judge stores every setting and returns it on the owning language`() {
    val result = createJudge()
    assertSuccess(result, "CreateJudgeConfigurationSuccess")
    val language = result["problemLanguage"]
    assertEquals(configurationId, language["id"].asString())
    val judge = language["judgeConfiguration"]
    assertEquals("fun main() = judge()", judge["testDriverCode"].asString())
    assertTrue(judge["checkerSource"].isNull)
    assertEquals(1000, judge["timeLimitMs"].asInt())
    assertEquals(256, judge["memoryLimitMb"].asInt())

    val stored = storedJudge(configuration.id)!!
    assertEquals(globalIdUtil.toGlobalId(GraphqlJudgeConfiguration::class, stored.id), judge["id"].asString())
    assertEquals("fun main() = judge()", stored.testDriverCode)
    assertNull(stored.checkerSource)
    assertEquals(1000, stored.timeLimitMs)
    assertEquals(256, stored.memoryLimitMb)
    assertEquals(configuration.id, stored.problemLanguageId)
    assertNull(storedJudge(otherConfiguration.id))
    assertEquals(configuration.updatedAt, storedConfiguration(configuration.id).updatedAt)

    assertValidation(createJudge(), "problemLanguageId")
    assertEquals(1, storedJudges().size)
  }

  @Test
  fun `partial updates change only supplied settings and empty inputs are no-ops`() {
    val created = createdJudge()
    val judgeId = judgeGlobalId(created)

    val result = updateJudge(judgeId, mapOf("timeLimitMs" to 2500))
    assertSuccess(result, "UpdateJudgeConfigurationSuccess")
    val judge = result["judgeConfiguration"]
    assertEquals(judgeId, judge["id"].asString())
    assertEquals(2500, judge["timeLimitMs"].asInt())
    assertEquals(256, judge["memoryLimitMb"].asInt())
    assertEquals(created.testDriverCode, judge["testDriverCode"].asString())

    val stored = storedJudge(configuration.id)!!
    assertEquals(2500, stored.timeLimitMs)
    assertEquals(256, stored.memoryLimitMb)
    assertEquals(created.testDriverCode, stored.testDriverCode)
    assertEquals(created.problemLanguageId, stored.problemLanguageId)

    // Explicit null clears the checker, so it is the one field excluded from this no-op check.
    assertSuccess(updateJudge(judgeId), "UpdateJudgeConfigurationSuccess")
    assertSuccess(updateJudge(judgeId, mapOf(
      "testDriverCode" to null,
      "timeLimitMs" to null,
      "memoryLimitMb" to null,
    )), "UpdateJudgeConfigurationSuccess")
    assertEquals(stored, storedJudge(configuration.id))
  }

  @Test
  fun `invalid settings report field errors and reject the whole mutation`() {
    assertValidation(createJudge(mapOf("testDriverCode" to " ")), "testDriverCode")
    assertValidation(createJudge(mapOf("testDriverCode" to "x".repeat(50_001))), "testDriverCode")
    assertValidation(createJudge(mapOf("checkerSource" to "x".repeat(50_001))), "checkerSource")
    assertValidation(createJudge(mapOf("checkerSource" to "fun check() = true")), "checkerSource")
    assertValidation(createJudge(mapOf("timeLimitMs" to 0)), "timeLimitMs")
    assertValidation(createJudge(mapOf("timeLimitMs" to 60_001)), "timeLimitMs")
    assertValidation(createJudge(mapOf("memoryLimitMb" to 0)), "memoryLimitMb")
    assertValidation(createJudge(mapOf("memoryLimitMb" to 8_193)), "memoryLimitMb")
    assertTrue(storedJudges().isEmpty())

    val boundaries = createJudge(mapOf(
      "testDriverCode" to "x".repeat(50_000),
      "timeLimitMs" to 60_000,
      "memoryLimitMb" to 8_192,
    ))
    assertSuccess(boundaries, "CreateJudgeConfigurationSuccess")
    val created = storedJudge(configuration.id)!!
    val judgeId = judgeGlobalId(created)

    assertValidation(updateJudge(judgeId, mapOf("testDriverCode" to "x".repeat(50_001))), "testDriverCode")
    assertValidation(updateJudge(judgeId, mapOf("checkerSource" to "fun check() = true")), "checkerSource")
    assertValidation(updateJudge(judgeId, mapOf("timeLimitMs" to 0)), "timeLimitMs")
    assertValidation(updateJudge(judgeId, mapOf("memoryLimitMb" to 8_193)), "memoryLimitMb")
    assertValidation(updateJudge(judgeId, mapOf("timeLimitMs" to 2500, "memoryLimitMb" to 0)), "memoryLimitMb")
    assertEquals(created, storedJudge(configuration.id))

    assertSuccess(updateJudge(judgeId, mapOf("timeLimitMs" to 1, "memoryLimitMb" to 1)), "UpdateJudgeConfigurationSuccess")
    assertEquals(1, storedJudge(configuration.id)!!.timeLimitMs)
  }

  @Test
  fun `custom checker problems require checker code and exact JSON problems reject it`() {
    entClient.problems.update(problem.id) { checkerKind = ProblemCheckerKind.CUSTOM }.save(fixtureContext).getOrThrow()
    assertValidation(createJudge(), "checkerSource")
    assertValidation(createJudge(mapOf("checkerSource" to " ")), "checkerSource")
    assertTrue(storedJudges().isEmpty())

    val result = createJudge(mapOf("checkerSource" to "fun check() = true"))
    assertSuccess(result, "CreateJudgeConfigurationSuccess")
    assertEquals("fun check() = true", result["problemLanguage"]["judgeConfiguration"]["checkerSource"].asString())
    val judgeId = judgeGlobalId(storedJudge(configuration.id)!!)

    assertValidation(updateJudge(judgeId, mapOf("checkerSource" to " ")), "checkerSource")
    assertValidation(updateJudge(judgeId, mapOf("checkerSource" to null)), "checkerSource")
    assertSuccess(updateJudge(judgeId, mapOf("checkerSource" to "fun check() = false")), "UpdateJudgeConfigurationSuccess")
    assertEquals("fun check() = false", storedJudge(configuration.id)!!.checkerSource)

    // A problem that moves back to exact JSON leaves its judge invalid until the checker is cleared.
    entClient.problems.update(problem.id) { checkerKind = ProblemCheckerKind.EXACT_JSON }.save(fixtureContext).getOrThrow()
    assertValidation(updateJudge(judgeId, mapOf("checkerSource" to "fun check() = true")), "checkerSource")
    assertValidation(updateJudge(judgeId, mapOf("timeLimitMs" to 5)), "checkerSource")
    assertEquals("fun check() = false", storedJudge(configuration.id)!!.checkerSource)
    assertSuccess(updateJudge(judgeId, mapOf("checkerSource" to null, "timeLimitMs" to 5)), "UpdateJudgeConfigurationSuccess")
    val cleared = storedJudge(configuration.id)!!
    assertNull(cleared.checkerSource)
    assertEquals(5, cleared.timeLimitMs)
  }

  @Test
  fun `malformed and wrong-type IDs are validation failures while missing targets are not found`() {
    assertValidation(createJudge(mapOf("problemLanguageId" to "not!!base64")), "problemLanguageId")
    assertValidation(createJudge(mapOf("problemLanguageId" to problemId)), "problemLanguageId")
    val missingLanguage = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, Long.MAX_VALUE)
    assertEquals("ProblemNotFound", createJudge(mapOf("problemLanguageId" to missingLanguage))["__typename"].asString())

    assertValidation(updateJudge("not!!base64"), "id")
    assertValidation(updateJudge(configurationId), "id")
    val missingJudge = globalIdUtil.toGlobalId(GraphqlJudgeConfiguration::class, Long.MAX_VALUE)
    assertEquals("ProblemNotFound", updateJudge(missingJudge)["__typename"].asString())
    assertTrue(storedJudges().isEmpty())
  }

  @Test
  fun `unavailable problems and disabled languages are not found for creation and updates`() {
    val judgeId = judgeGlobalId(createdJudge())
    val now = Instant.now()
    val availability = listOf(
      null to null,
      now.plusSeconds(3600) to null,
      now to now,
    )
    for ((publishedAt, archivedAt) in availability) {
      entClient.problems.update(problem.id) {
        this.publishedAt = publishedAt
        this.archivedAt = archivedAt
      }.save(fixtureContext).getOrThrow()

      assertEquals("ProblemNotFound", updateJudge(judgeId, mapOf("timeLimitMs" to 5))["__typename"].asString())
      assertEquals("ProblemNotFound", createJudge(mapOf("problemLanguageId" to otherConfigurationId))["__typename"].asString())
    }

    entClient.problems.update(problem.id) {
      publishedAt = now
      archivedAt = null
    }.save(fixtureContext).getOrThrow()
    entClient.languages.update(configuration.languageId) { enabled = false }.save(fixtureContext).getOrThrow()
    entClient.languages.update(otherConfiguration.languageId) { enabled = false }.save(fixtureContext).getOrThrow()
    assertEquals("ProblemNotFound", updateJudge(judgeId, mapOf("timeLimitMs" to 5))["__typename"].asString())
    assertEquals("ProblemNotFound", createJudge(mapOf("problemLanguageId" to otherConfigurationId))["__typename"].asString())

    assertEquals(1000, storedJudge(configuration.id)!!.timeLimitMs)
    assertNull(storedJudge(otherConfiguration.id))
  }

  @Test
  fun `judge settings are readable by admins only`() {
    createdJudge()
    val (_, ordinarySession) = login(UserRole.USER)

    val adminView = readProblem(adminSession)["languageConfigurations"].associateBy { it["id"].asString() }
    val judge = adminView.getValue(configurationId)["judgeConfiguration"]
    assertEquals("fun main() = judge()", judge["testDriverCode"].asString())
    assertEquals(1000, judge["timeLimitMs"].asInt())
    assertTrue(adminView.getValue(otherConfigurationId)["judgeConfiguration"].isNull)

    for (session in listOf(null, ordinarySession)) {
      val view = readProblem(session)
      assertEquals(2, view["languageConfigurations"].size())
      assertTrue(view["languageConfigurations"].all { it["judgeConfiguration"].isNull }, view.toString())
      assertFalse(view.toString().contains("fun main() = judge()"))
    }

    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
    val revokedView = readProblem(adminSession)
    assertTrue(revokedView["languageConfigurations"].all { it["judgeConfiguration"].isNull }, revokedView.toString())
  }

  @Test
  fun `anonymous ordinary and revoked viewers receive forbidden results without writes`() {
    val (_, ordinarySession) = login(UserRole.USER)
    for (session in listOf(null, ordinarySession)) {
      assertEquals("ProblemForbidden", createJudge(session = session)["__typename"].asString())
    }
    assertTrue(storedJudges().isEmpty())

    val judgeId = judgeGlobalId(createdJudge())
    for (session in listOf(null, ordinarySession)) {
      assertEquals("ProblemForbidden", updateJudge(judgeId, mapOf("timeLimitMs" to 5), session)["__typename"].asString())
    }

    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
    assertEquals("ProblemForbidden", updateJudge(judgeId, mapOf("timeLimitMs" to 5))["__typename"].asString())
    assertEquals("ProblemForbidden", createJudge(mapOf("problemLanguageId" to otherConfigurationId))["__typename"].asString())
    assertEquals(1000, storedJudge(configuration.id)!!.timeLimitMs)
    assertNull(storedJudge(otherConfiguration.id))

    entClient.users.update(adminId) { role = UserRole.ADMIN }.save(fixtureContext).getOrThrow()
    assertSuccess(updateJudge(judgeId, mapOf("timeLimitMs" to 5)), "UpdateJudgeConfigurationSuccess")
    assertSuccess(createJudge(mapOf("problemLanguageId" to otherConfigurationId)), "CreateJudgeConfigurationSuccess")
  }

  @Test
  fun `Ent policies deny non-admin reads and writes`() {
    val judge = createdJudge()
    val (ordinaryId, _) = login(UserRole.USER)
    val securityContext = adminSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext
    SecurityContextHolder.setContext(securityContext)
    try {
      val adminViewer = ViewerContext(Viewer.User(adminId))
      assertEquals(judge.id, entClient.judgeConfigurations.findById(adminViewer, judge.id).getOrThrow()!!.id)

      for (viewer in listOf(Viewer.Anonymous, Viewer.User(ordinaryId), Viewer.User(adminId + 1000))) {
        assertDenied(ViewerContext(viewer), judge.id)
      }

      entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
      assertDenied(adminViewer, judge.id)
    } finally {
      SecurityContextHolder.clearContext()
    }
    assertEquals(1000, storedJudge(configuration.id)!!.timeLimitMs)
    assertNull(storedJudge(otherConfiguration.id))
  }

  private fun assertDenied(context: ViewerContext, judgeId: Long) {
    assertThrows(EntPrivacyDeniedException::class.java) {
      entClient.judgeConfigurations.findById(context, judgeId).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.judgeConfigurations.update(judgeId) { timeLimitMs = 5 }.save(context).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.judgeConfigurations.create {
        problemLanguageId = otherConfiguration.id
        testDriverCode = "fun main() = judge()"
        timeLimitMs = 1000
        memoryLimitMb = 256
      }.save(context).getOrThrow()
    }
  }

  private fun assertSuccess(result: JsonNode, typename: String) {
    assertEquals(typename, result["__typename"].asString(), result.toString())
  }

  private fun assertValidation(result: JsonNode, field: String) {
    assertEquals("ProblemValidationFailure", result["__typename"].asString(), result.toString())
    assertTrue(result["fieldErrors"].any { it["field"].asString() == field }, result.toString())
  }

  private fun createdJudge(): JudgeConfiguration {
    assertSuccess(createJudge(), "CreateJudgeConfigurationSuccess")
    return storedJudge(configuration.id)!!
  }

  private fun judgeGlobalId(judge: JudgeConfiguration) =
    globalIdUtil.toGlobalId(GraphqlJudgeConfiguration::class, judge.id)

  private fun storedJudge(problemLanguageId: Long): JudgeConfiguration? =
    entClient.judgeConfigurations.indexes.problemLanguageId(problemLanguageId).find(fixtureContext).getOrThrow()

  private fun storedJudges(): List<JudgeConfiguration> = entClient.judgeConfigurations.query {
    where(JudgeConfiguration.problemLanguageId `in` listOf(configuration.id, otherConfiguration.id))
  }.all(fixtureContext).getOrThrow()

  private fun storedConfiguration(id: Long) = entClient.problemLanguages.findById(fixtureContext, id).getOrThrow()!!

  private fun createLanguage(): Language = entClient.languages.create {
    key = "judge-${UUID.randomUUID()}"
    displayName = "Test language"
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createConfiguration(language: Language): ProblemLanguage = entClient.problemLanguages.create {
    problemId = problem.id
    languageId = language.id
    starterCode = "initial source"
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun login(role: UserRole): Pair<Long, MockHttpSession> {
    val email = "${UUID.randomUUID()}@example.com"
    val user = entClient.users.create {
      this.email = email
      hashedPassword = passwordEncoder.encode("test-password")
      this.role = role
    }.saveAndLoad(fixtureContext).getOrThrow()
    val result = mvc.perform(post("/login")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .param("username", email)
      .param("password", "test-password")
    ).andReturn()
    assertEquals("/", result.response.redirectedUrl)
    return user.id to (result.request.session as MockHttpSession)
  }

  private val judgeFields = "id testDriverCode checkerSource timeLimitMs memoryLimitMb"

  private fun createJudge(
    fields: Map<String, Any?> = emptyMap(),
    session: MockHttpSession? = adminSession,
  ): JsonNode = graphql(
    """
      mutation CreateJudge(${'$'}input: CreateJudgeConfigurationInput!) {
        createJudgeConfiguration(input: ${'$'}input) {
          __typename
          ... on CreateJudgeConfigurationSuccess {
            problemLanguage { id judgeConfiguration { $judgeFields } }
          }
          ... on ProblemFailure { message }
          ... on ProblemValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent(),
    mapOf("input" to (mapOf("problemLanguageId" to configurationId) + validSettings + fields)),
    session,
  )["createJudgeConfiguration"]

  private fun updateJudge(
    id: String,
    fields: Map<String, Any?> = emptyMap(),
    session: MockHttpSession? = adminSession,
  ): JsonNode = graphql(
    """
      mutation UpdateJudge(${'$'}input: UpdateJudgeConfigurationInput!) {
        updateJudgeConfiguration(input: ${'$'}input) {
          __typename
          ... on UpdateJudgeConfigurationSuccess { judgeConfiguration { $judgeFields } }
          ... on ProblemFailure { message }
          ... on ProblemValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent(),
    mapOf("input" to (mapOf("id" to id) + fields)),
    session,
  )["updateJudgeConfiguration"]

  private fun readProblem(session: MockHttpSession?): JsonNode = graphql(
    """
      query ReadJudges(${'$'}slug: String!) {
        problem(slug: ${'$'}slug) {
          languageConfigurations { id judgeConfiguration { $judgeFields } }
        }
      }
    """.trimIndent(),
    mapOf("slug" to problem.slug),
    session,
  )["problem"]

  private fun graphql(query: String, variables: Map<String, Any?>, session: MockHttpSession?): JsonNode {
    val request = post("/graphql")
      .contentType("application/json")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .content(objectMapper.writeValueAsString(mapOf("query" to query, "variables" to variables)))
    session?.let { request.session(it) }
    var result = mvc.perform(request).andReturn()
    if (result.request.isAsyncStarted) {
      result = mvc.perform(asyncDispatch(result)).andReturn()
    }
    assertEquals(200, result.response.status, result.response.contentAsString)
    val response = objectMapper.readTree(result.response.contentAsString)
    assertFalse(response.has("errors"), response.toString())
    return response["data"]
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

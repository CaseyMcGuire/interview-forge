package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Problem
import com.application.ent.TestCase
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntPrivacyDeniedException
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.application.graphql.types.Problem as GraphqlProblem
import com.application.graphql.types.ProblemExample as GraphqlProblemExample

@Testcontainers
@SpringBootTest
class ProblemHiddenTestCaseIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect hidden-test fixtures in an isolated test database"
  )

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var applicationContext: WebApplicationContext
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var objectMapper: ObjectMapper
  @Autowired lateinit var globalIdUtil: GlobalIdUtil
  @Autowired @Qualifier("springSecurityFilterChain") lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc
  private var adminId: Long = 0
  private lateinit var adminSession: MockHttpSession
  private lateinit var problem: Problem

  private val problemId get() = globalIdUtil.toGlobalId(GraphqlProblem::class, problem.id)

  @BeforeEach
  fun setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()

    val login = login(UserRole.ADMIN)
    adminId = login.first
    adminSession = login.second
    problem = createProblem()
  }

  @Test
  fun `admins append after public and hidden cases without changing existing content or exposing hidden data`() {
    val example = createCase(2, TestCaseVisibility.EXAMPLE)
    val hidden = createCase(8, TestCaseVisibility.HIDDEN)
    createCase(50, TestCaseVisibility.HIDDEN, createProblem().id)
    val input = "{\"args\": [1, 2]}"
    val explanation = "A private edge case."

    val result = createHiddenCase(mapOf(
      "inputJson" to input,
      "expectedOutputJson" to "3",
      "explanationMarkdown" to explanation,
    ))

    assertSuccess(result)
    assertEquals(problemId, result["problem"]["id"].asString())
    assertEquals(listOf(2), result["problem"]["examples"].toList().map { it["position"].asInt() })
    val stored = storedCases()
    assertEquals(listOf(2, 8, 9), stored.map { it.position })
    val created = stored.last()
    assertEquals(TestCaseVisibility.HIDDEN, created.visibility)
    assertEquals(Json.parseToJsonElement(input), created.inputJson)
    assertEquals(JsonPrimitive(3), created.expectedOutputJson)
    assertEquals(explanation, created.explanationMarkdown)
    assertEquals(example, stored[0])
    assertEquals(hidden, stored[1])
    assertEquals(problem.updatedAt, entClient.problems.findById(fixtureContext, problem.id).getOrThrow()!!.updatedAt)

    for (session in listOf(null, adminSession)) {
      val publicResult = graphql(
        """
          query PublicProblem(${'$'}slug: String!) {
            problem(slug: ${'$'}slug) {
              examples { id position inputJson expectedOutputJson explanationMarkdown }
            }
          }
        """.trimIndent(),
        mapOf("slug" to problem.slug),
        session,
      )
      val examples = publicResult["problem"]["examples"]
      assertEquals(listOf(2), examples.toList().map { it["position"].asInt() })
      assertFalse(examples.toString().contains(explanation))
    }
  }

  @Test
  fun `empty and hidden-only suites accept JSON null and omitted or null explanations`() {
    assertSuccess(createHiddenCase(mapOf("inputJson" to "null", "expectedOutputJson" to "null")))
    assertSuccess(createHiddenCase(mapOf("explanationMarkdown" to null)))

    val stored = storedCases()
    assertEquals(listOf(0, 1), stored.map { it.position })
    assertEquals(JsonNull, stored[0].inputJson)
    assertEquals(JsonNull, stored[0].expectedOutputJson)
    assertTrue(stored.all { it.visibility == TestCaseVisibility.HIDDEN && it.explanationMarkdown == null })
  }

  @Test
  fun `content limits apply to compact JSON and accept the exact boundaries`() {
    val boundaryJson = JsonPrimitive("x".repeat(19_998)).toString()
    assertSuccess(createHiddenCase(mapOf(
      "inputJson" to " $boundaryJson \n",
      "expectedOutputJson" to boundaryJson,
      "explanationMarkdown" to "x".repeat(10_000),
    )))

    val stored = storedCases().single()
    assertEquals(20_000, stored.inputJson.toString().length)
    assertEquals(20_000, stored.expectedOutputJson.toString().length)
    assertEquals(10_000, stored.explanationMarkdown!!.length)
  }

  @Test
  fun `invalid JSON and oversized content return field errors without saving a case`() {
    val oversizedJson = JsonPrimitive("x".repeat(19_999)).toString()
    val invalidFields = listOf(
      "inputJson" to "{broken",
      "inputJson" to " ",
      "expectedOutputJson" to "{broken",
      "inputJson" to oversizedJson,
      "expectedOutputJson" to oversizedJson,
      "explanationMarkdown" to "x".repeat(10_001),
    )

    for ((field, value) in invalidFields) {
      assertValidation(createHiddenCase(mapOf(field to value)), field)
      assertTrue(storedCases().isEmpty())
    }

    assertSuccess(createHiddenCase())
    assertEquals(0, storedCases().single().position)
  }

  @Test
  fun `malformed and wrong-type IDs are validation failures and missing problems are not found`() {
    for (id in listOf("not!!base64", globalIdUtil.toGlobalId(GraphqlProblemExample::class, problem.id))) {
      assertValidation(createHiddenCase(mapOf("problemId" to id)), "problemId")
    }

    val missing = globalIdUtil.toGlobalId(GraphqlProblem::class, Long.MAX_VALUE)
    assertEquals("ProblemNotFound", createHiddenCase(mapOf("problemId" to missing))["__typename"].asString())
    assertTrue(storedCases().isEmpty())
  }

  @Test
  fun `unpublished future and archived problems cannot receive hidden cases`() {
    val now = Instant.now()
    for ((publishedAt, archivedAt) in listOf(null to null, now.plusSeconds(3600) to null, now to now)) {
      entClient.problems.update(problem.id) {
        this.publishedAt = publishedAt
        this.archivedAt = archivedAt
      }.save(fixtureContext).getOrThrow()

      assertEquals("ProblemNotFound", createHiddenCase()["__typename"].asString())
      assertTrue(storedCases().isEmpty())
    }
  }

  @Test
  fun `anonymous ordinary revoked and deleted admins cannot create hidden cases`() {
    val (_, ordinary) = login(UserRole.USER)
    for (session in listOf(null, ordinary)) {
      assertEquals("ProblemForbidden", createHiddenCase(session = session)["__typename"].asString())
    }

    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
    assertEquals("ProblemForbidden", createHiddenCase()["__typename"].asString())

    val (deletedId, deletedSession) = login(UserRole.ADMIN)
    entClient.users.deleteById(fixtureContext, deletedId).getOrThrow()
    assertEquals("ProblemForbidden", createHiddenCase(session = deletedSession)["__typename"].asString())
    assertTrue(storedCases().isEmpty())

    entClient.users.update(adminId) { role = UserRole.ADMIN }.save(fixtureContext).getOrThrow()
    assertSuccess(createHiddenCase())
  }

  @Test
  fun `hidden reads require a matching current admin regardless of problem publication or archival`() {
    val hidden = createCase(0, TestCaseVisibility.HIDDEN)
    val (ordinaryId, _) = login(UserRole.USER)
    val securityContext = adminSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext
    SecurityContextHolder.setContext(securityContext)
    try {
      val adminViewer = ViewerContext(Viewer.User(adminId))
      assertEquals(hidden.id, entClient.testCases.findById(adminViewer, hidden.id).getOrThrow()!!.id)
      for (viewer in listOf(Viewer.Anonymous, Viewer.User(ordinaryId))) {
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.testCases.findById(ViewerContext(viewer), hidden.id).getOrThrow()
        }
      }

      entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.testCases.findById(adminViewer, hidden.id).getOrThrow()
      }

      entClient.users.update(adminId) { role = UserRole.ADMIN }.save(fixtureContext).getOrThrow()
      val now = Instant.now()
      for ((publishedAt, archivedAt) in listOf(null to null, now.plusSeconds(3600) to null, now to now)) {
        entClient.problems.update(problem.id) {
          this.publishedAt = publishedAt
          this.archivedAt = archivedAt
        }.save(fixtureContext).getOrThrow()

        assertEquals(hidden.id, entClient.testCases.findById(adminViewer, hidden.id).getOrThrow()!!.id)
        for (viewer in listOf(Viewer.Anonymous, Viewer.User(ordinaryId))) {
          assertThrows(EntPrivacyDeniedException::class.java) {
            entClient.testCases.findById(ViewerContext(viewer), hidden.id).getOrThrow()
          }
        }
      }
    } finally {
      SecurityContextHolder.clearContext()
    }
  }

  @Test
  fun `concurrent additions to an empty suite receive distinct consecutive positions`() {
    val executor = Executors.newFixedThreadPool(4)
    val ready = CountDownLatch(4)
    val start = CountDownLatch(1)
    try {
      val results = (0 until 4).map { index ->
        executor.submit<JsonNode> {
          ready.countDown()
          check(start.await(10, TimeUnit.SECONDS))
          createHiddenCase(mapOf("inputJson" to "[$index]"))
        }
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS))
      start.countDown()
      results.forEach { assertSuccess(it.get(30, TimeUnit.SECONDS)) }

      val stored = storedCases()
      assertEquals(listOf(0, 1, 2, 3), stored.map { it.position })
      assertEquals((0 until 4).map { "[$it]" }.toSet(), stored.map { it.inputJson.toString() }.toSet())
      assertTrue(stored.all { it.visibility == TestCaseVisibility.HIDDEN })
    } finally {
      start.countDown()
      executor.shutdownNow()
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
    }
  }

  @Test
  fun `a full position range is rejected without overflowing or changing cases`() {
    val original = createCase(Int.MAX_VALUE, TestCaseVisibility.HIDDEN)
    assertValidation(createHiddenCase(), "problemId")
    assertEquals(listOf(original), storedCases())
  }

  private fun assertSuccess(result: JsonNode) {
    assertEquals("CreateProblemHiddenTestCaseSuccess", result["__typename"].asString(), result.toString())
  }

  private fun assertValidation(result: JsonNode, field: String) {
    assertEquals("ProblemValidationFailure", result["__typename"].asString(), result.toString())
    assertTrue(result["fieldErrors"].any { it["field"].asString() == field }, result.toString())
  }

  private fun storedCases(): List<TestCase> = entClient.testCases.query {
    where(TestCase.problemId eq problem.id)
    orderBy(TestCase.position.asc())
  }.all(fixtureContext).getOrThrow()

  private fun createProblem(): Problem = entClient.problems.create {
    slug = "hidden-${UUID.randomUUID()}"
    title = "Hidden test problem"
    statementMarkdown = "Add two numbers."
    difficulty = ProblemDifficulty.EASY
    createdByUserId = adminId
    publishedAt = Instant.now().minusSeconds(60)
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createCase(position: Int, visibility: TestCaseVisibility, problemId: Long = problem.id): TestCase =
    entClient.testCases.create {
      this.problemId = problemId
      this.position = position
      this.visibility = visibility
      inputJson = Json.parseToJsonElement("[1,2]")
      expectedOutputJson = JsonPrimitive(3)
      explanationMarkdown = "Existing case."
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

  private fun createHiddenCase(
    fields: Map<String, Any?> = emptyMap(),
    session: MockHttpSession? = adminSession,
  ): JsonNode = graphql(
    """
      mutation CreateHiddenCase(${'$'}input: CreateProblemHiddenTestCaseInput!) {
        createProblemHiddenTestCase(input: ${'$'}input) {
          __typename
          ... on CreateProblemHiddenTestCaseSuccess {
            problem { id examples { id position inputJson expectedOutputJson explanationMarkdown } }
          }
          ... on ProblemFailure { message }
          ... on ProblemValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent(),
    mapOf("input" to (mapOf(
      "problemId" to problemId,
      "inputJson" to "[]",
      "expectedOutputJson" to "[]",
    ) + fields)),
    session,
  )["createProblemHiddenTestCase"]

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

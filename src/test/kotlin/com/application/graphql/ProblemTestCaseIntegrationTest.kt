package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Problem
import com.application.ent.TestCase
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import entkt.runtime.privacy.ViewerContext
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
import org.springframework.security.crypto.password.PasswordEncoder
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
import com.application.graphql.types.ProblemExample as GraphqlProblemExample
import com.application.graphql.types.ProblemTestCase as GraphqlProblemTestCase

@Testcontainers
@SpringBootTest
class ProblemTestCaseIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect test-case editing fixtures in an isolated test database"
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
  private lateinit var testCase: TestCase
  private lateinit var otherTestCase: TestCase
  private lateinit var publicTestCase: TestCase

  private val testCaseId get() = globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, testCase.id)

  @BeforeEach
  fun setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()

    val login = login(UserRole.ADMIN)
    adminId = login.first
    adminSession = login.second
    problem = createProblem()
    testCase = createTestCase(9)
    otherTestCase = createTestCase(2)
    publicTestCase = createTestCase(5, visibility = TestCaseVisibility.EXAMPLE)
  }

  @Test
  fun `admins list tests in position order and load one without including public or other problem cases`() {
    createTestCase(1, problemId = createProblem().id)

    val result = queryTestCases()

    assertEquals(listOf(2, 9), result["testCases"].toList().map { it["position"].asInt() })
    assertEquals(testCaseId, result["testCase"]["id"].asString())
    assertEquals(testCase.inputJson.toString(), result["testCase"]["inputJson"].asString())
    assertEquals(testCase.expectedOutputJson.toString(), result["testCase"]["expectedOutputJson"].asString())
    assertEquals(testCase.explanationMarkdown, result["testCase"]["explanationMarkdown"].asString())
    assertEquals(listOf(5), result["examples"].toList().map { it["position"].asInt() })
  }

  @Test
  fun `a problem with only public examples has an empty test list`() {
    val otherProblem = createProblem()
    createTestCase(0, problemId = otherProblem.id, visibility = TestCaseVisibility.EXAMPLE)

    val result = queryTestCases(slug = otherProblem.slug)

    assertTrue(result["testCases"].isArray)
    assertEquals(0, result["testCases"].size())
    assertTrue(result["testCase"].isNull)
    assertEquals(1, result["examples"].size())
  }

  @Test
  fun `single test lookup rejects public cases other problems missing cases and invalid IDs`() {
    val otherProblemCase = createTestCase(0, problemId = createProblem().id)
    val invalidIds = listOf(
      "not!!base64",
      globalIdUtil.toGlobalId(GraphqlProblemExample::class, publicTestCase.id),
      globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, publicTestCase.id),
      globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, otherProblemCase.id),
      globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, Long.MAX_VALUE),
    )

    for (id in invalidIds) {
      val result = queryTestCases(id = id)
      assertTrue(result["testCase"].isNull, id)
      assertEquals(2, result["testCases"].size())
    }
  }

  @Test
  fun `anonymous ordinary revoked and deleted admins cannot read or update tests`() {
    val (_, ordinarySession) = login(UserRole.USER)
    for (session in listOf(null, ordinarySession)) {
      assertAccessDenied(session)
    }

    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
    assertAccessDenied(adminSession)

    val (deletedId, deletedSession) = login(UserRole.ADMIN)
    entClient.users.deleteById(fixtureContext, deletedId).getOrThrow()
    assertAccessDenied(deletedSession)
    assertEquals(testCase, storedTestCase(testCase.id))

    entClient.users.update(adminId) { role = UserRole.ADMIN }.save(fixtureContext).getOrThrow()
    assertEquals(2, queryTestCases()["testCases"].size())
  }

  @Test
  fun `admins can update tests on unpublished future and archived problems without making the problems public`() {
    val (_, ordinarySession) = login(UserRole.USER)
    val now = Instant.now()
    for ((publishedAt, archivedAt) in listOf(null to null, now.plusSeconds(3600) to null, now to now)) {
      entClient.problems.update(problem.id) {
        this.publishedAt = publishedAt
        this.archivedAt = archivedAt
      }.save(fixtureContext).getOrThrow()

      for (session in listOf(null, ordinarySession)) {
        assertTrue(queryTestCases(session = session).isNull)
        assertEquals("ProblemForbidden", updateTestCase(session = session)["__typename"].asString())
      }

      for (target in listOf(testCase, publicTestCase)) {
        val result = updateTestCase(mapOf(
          "id" to globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, target.id),
          "inputJson" to "[8,9]",
          "expectedOutputJson" to "17",
        ))
        assertSuccess(result)
        val stored = storedTestCase(target.id)
        assertEquals(Json.parseToJsonElement("[8,9]"), stored.inputJson)
        assertEquals(JsonPrimitive(17), stored.expectedOutputJson)
        assertEquals(target.visibility, stored.visibility)
        assertEquals(target.position, stored.position)
      }

      val storedProblem = entClient.problems.findById(fixtureContext, problem.id).getOrThrow()!!
      assertEquals(publishedAt, storedProblem.publishedAt)
      assertEquals(archivedAt, storedProblem.archivedAt)
    }
  }

  @Test
  fun `updates save the client answer and preserve test identity ordering and unrelated content`() {
    val input = "{\"args\":[9007199254740993,1.0]}"
    val output = "9007199254740993"
    val explanation = "A revised explanation."

    // The problem has no reference solution configured; saving does not execute code.
    val result = updateTestCase(mapOf(
      "inputJson" to input,
      "expectedOutputJson" to output,
      "explanationMarkdown" to explanation,
    ))

    assertSuccess(result)
    assertEquals(testCaseId, result["testCase"]["id"].asString())
    assertEquals(output, result["testCase"]["expectedOutputJson"].asString())
    assertEquals(explanation, result["testCase"]["explanationMarkdown"].asString())

    val stored = storedTestCase(testCase.id)
    assertEquals(Json.parseToJsonElement(input), stored.inputJson)
    assertEquals(Json.parseToJsonElement(output), stored.expectedOutputJson)
    assertEquals(explanation, stored.explanationMarkdown)
    assertEquals(testCase.position, stored.position)
    assertEquals(testCase.visibility, stored.visibility)
    assertEquals(testCase.problemId, stored.problemId)
    assertEquals(testCase.createdAt, stored.createdAt)
    assertEquals(otherTestCase, storedTestCase(otherTestCase.id))
    assertEquals(publicTestCase, storedTestCase(publicTestCase.id))
    assertEquals(problem, entClient.problems.findById(fixtureContext, problem.id).getOrThrow())
    assertEquals(result["testCase"], queryTestCases()["testCase"])
  }

  @Test
  fun `JSON null is a value and omitted or null explanations clear the stored text`() {
    for (explanationFields in listOf(emptyMap(), mapOf("explanationMarkdown" to null))) {
      entClient.testCases.update(testCase.id) {
        explanationMarkdown = "An explanation to clear."
      }.save(fixtureContext).getOrThrow()

      val result = updateTestCase(mapOf("inputJson" to "null", "expectedOutputJson" to "null") + explanationFields)

      assertSuccess(result)
      assertEquals("null", result["testCase"]["expectedOutputJson"].asString())
      val stored = storedTestCase(testCase.id)
      assertEquals(JsonNull, stored.inputJson)
      assertEquals(JsonNull, stored.expectedOutputJson)
      assertNull(stored.explanationMarkdown)
    }
  }

  @Test
  fun `compact JSON and explanation size boundaries are accepted`() {
    val boundaryJson = JsonPrimitive("x".repeat(19_998)).toString()

    assertSuccess(updateTestCase(mapOf(
      "inputJson" to " $boundaryJson \n",
      "expectedOutputJson" to boundaryJson,
      "explanationMarkdown" to "x".repeat(10_000),
    )))

    val stored = storedTestCase(testCase.id)
    assertEquals(20_000, stored.inputJson.toString().length)
    assertEquals(20_000, stored.expectedOutputJson.toString().length)
    assertEquals(10_000, stored.explanationMarkdown!!.length)
  }

  @Test
  fun `invalid JSON and oversized content reject the entire update with a field error`() {
    val invalidJson = listOf("{broken", " ", "bareword", "NaN", "01", "1 2", "{\"x\":1,\"x\":2}")
    val oversizedJson = JsonPrimitive("x".repeat(19_999)).toString()
    val invalidFields = listOf("inputJson", "expectedOutputJson").flatMap { field ->
      (invalidJson + oversizedJson).map { field to it }
    } + ("explanationMarkdown" to "x".repeat(10_001))

    for ((field, value) in invalidFields) {
      assertValidation(updateTestCase(mapOf(field to value)), field)
      assertEquals(testCase, storedTestCase(testCase.id))
    }
  }

  @Test
  fun `updates reject invalid IDs and missing cases`() {
    for (id in listOf("not!!base64", globalIdUtil.toGlobalId(GraphqlProblemExample::class, publicTestCase.id))) {
      assertValidation(updateTestCase(mapOf("id" to id)), "id")
    }

    val missingId = globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, Long.MAX_VALUE)
    assertEquals("ProblemNotFound", updateTestCase(mapOf("id" to missingId))["__typename"].asString())

    assertEquals(publicTestCase, storedTestCase(publicTestCase.id))
    assertEquals(testCase, storedTestCase(testCase.id))
  }

  @Test
  fun `updates also support public cases without changing visibility or adding them to the test list`() {
    val publicId = globalIdUtil.toGlobalId(GraphqlProblemTestCase::class, publicTestCase.id)

    val result = updateTestCase(mapOf(
      "id" to publicId,
      "inputJson" to "[6,7]",
      "expectedOutputJson" to "13",
      "explanationMarkdown" to "A revised public example.",
    ))

    assertSuccess(result)
    assertEquals(publicId, result["testCase"]["id"].asString())
    val stored = storedTestCase(publicTestCase.id)
    assertEquals(Json.parseToJsonElement("[6,7]"), stored.inputJson)
    assertEquals(JsonPrimitive(13), stored.expectedOutputJson)
    assertEquals("A revised public example.", stored.explanationMarkdown)
    assertEquals(TestCaseVisibility.EXAMPLE, stored.visibility)
    assertEquals(publicTestCase.position, stored.position)
    assertEquals(publicTestCase.problemId, stored.problemId)
    assertEquals(testCase, storedTestCase(testCase.id))
    assertEquals(otherTestCase, storedTestCase(otherTestCase.id))
    assertEquals(listOf(2, 9), queryTestCases()["testCases"].toList().map { it["position"].asInt() })
  }

  private fun assertAccessDenied(session: MockHttpSession?) {
    val result = queryTestCases(session = session)
    assertTrue(result["testCases"].isNull)
    assertTrue(result["testCase"].isNull)
    assertEquals(1, result["examples"].size())
    assertEquals("ProblemForbidden", updateTestCase(session = session)["__typename"].asString())
  }

  private fun assertSuccess(result: JsonNode) {
    assertEquals("UpdateProblemTestCaseSuccess", result["__typename"].asString(), result.toString())
  }

  private fun assertValidation(result: JsonNode, field: String) {
    assertEquals("ProblemValidationFailure", result["__typename"].asString(), result.toString())
    assertTrue(result["fieldErrors"].any { it["field"].asString() == field }, result.toString())
  }

  private fun storedTestCase(id: Long): TestCase = entClient.testCases.findById(fixtureContext, id).getOrThrow()!!

  private fun createProblem(): Problem = entClient.problems.create {
    slug = "test-edit-${UUID.randomUUID()}"
    title = "Test case editing"
    statementMarkdown = "Add two numbers."
    difficulty = ProblemDifficulty.EASY
    createdByUserId = adminId
    publishedAt = Instant.now().minusSeconds(60)
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createTestCase(
    position: Int,
    problemId: Long = problem.id,
    visibility: TestCaseVisibility = TestCaseVisibility.HIDDEN,
  ): TestCase = entClient.testCases.create {
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

  private fun queryTestCases(
    slug: String = problem.slug,
    id: String = testCaseId,
    session: MockHttpSession? = adminSession,
  ): JsonNode = graphql(
    """
      query TestCases(${'$'}slug: String!, ${'$'}id: ID!) {
        problem(slug: ${'$'}slug) {
          testCases { id position inputJson expectedOutputJson explanationMarkdown }
          testCase(id: ${'$'}id) { id position inputJson expectedOutputJson explanationMarkdown }
          examples { id position }
        }
      }
    """.trimIndent(),
    mapOf("slug" to slug, "id" to id),
    session,
  )["problem"]

  private fun updateTestCase(
    fields: Map<String, Any?> = emptyMap(),
    session: MockHttpSession? = adminSession,
  ): JsonNode = graphql(
    """
      mutation UpdateTestCase(${'$'}input: UpdateProblemTestCaseInput!) {
        updateProblemTestCase(input: ${'$'}input) {
          __typename
          ... on UpdateProblemTestCaseSuccess {
            testCase { id position inputJson expectedOutputJson explanationMarkdown }
          }
          ... on ProblemFailure { message }
          ... on ProblemValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent(),
    mapOf("input" to (mapOf(
      "id" to testCaseId,
      "inputJson" to "[4,5]",
      "expectedOutputJson" to "9",
    ) + fields)),
    session,
  )["updateProblemTestCase"]

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

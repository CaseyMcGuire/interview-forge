package com.application.graphql

import com.application.ent.CustomTestCase
import com.application.ent.CustomInputSubmission
import com.application.ent.EntClient
import com.application.ent.Problem
import com.application.execution.RuntimeAvailability
import com.application.schema.CustomInputSubmissionOutcome
import com.application.schema.CustomInputSubmissionStatus
import com.application.schema.ProblemDifficulty
import com.application.schema.ProblemSubmissionStatus
import com.application.schema.ProblemSubmissionVerdict
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.security.ExecutionAccess
import com.application.services.CustomInputSubmissionService
import com.application.services.EnqueueCustomInputSubmissionOutcome
import com.application.services.ProblemSubmissionService
import com.application.services.SubmitSolutionOutcome
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import entkt.runtime.result.EntValidationException
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.application.graphql.types.CustomInputSubmission as GraphqlCustomInputSubmission
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage
import com.application.graphql.types.ProblemSubmission as GraphqlProblemSubmission

@Testcontainers
@SpringBootTest(properties = [
  "execution.runtimes.kotlin=test-runtime",
  "execution.max-active-submissions=4",
  "execution.max-active-submissions-per-user=2",
  "execution.max-custom-test-cases=3",
  "execution.custom-input-submission-lifetime=2h",
])
@Import(CustomInputSubmissionIntegrationTest.RuntimeConfiguration::class)
class CustomInputSubmissionIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated custom-run API fixtures")

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var applicationContext: WebApplicationContext

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var globalIdUtil: GlobalIdUtil

  @Autowired
  lateinit var runtime: TestRuntimeAvailability

  @Autowired
  lateinit var customInputSubmissionService: CustomInputSubmissionService

  @Autowired
  lateinit var problemSubmissionService: ProblemSubmissionService


  @Autowired
  @Qualifier("springSecurityFilterChain")
  lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc
  private lateinit var session: MockHttpSession
  private lateinit var problem: Problem
  private var userId = 0L
  private var configurationId = 0L
  private var judgeId = 0L

  private val publicConfigurationId get() = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configurationId)

  @BeforeEach
  fun setUp() {
    entClient.withTransaction { tx ->
      tx.customInputSubmissions.deleteMany(fixtures).getOrThrow()
      tx.problemSubmissionFailures.deleteMany(fixtures).getOrThrow()
      tx.problemSubmissions.deleteMany(fixtures).getOrThrow()
    }.getOrThrow()

    runtime.available = true
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()

    val login = login()
    userId = login.first
    session = login.second

    problem = entClient.problems.create {
      slug = "custom-run-${UUID.randomUUID()}"
      title = "Custom run fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
      publishedAt = Instant.now().minusSeconds(60)
    }.saveAndLoad(fixtures).getOrThrow()

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = problem.id
      languageId = language.id
      starterCode = "fun solve(value: Int) = value"
    }.saveAndLoad(fixtures).getOrThrow().id

    judgeId = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      testDriverCode = "private driver"
      referenceSolutionCode = "private reference solution"
      timeLimitMs = 1000
      memoryLimitMb = 256
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `each request creates its own owned run and ordered inputs without running code`() {
    val before = Instant.now()
    val id = assertSuccess(enqueueCustomInputSubmission(listOf("null", "  [1, 2]  ", "9007199254740993")))
    val run = storedRuns().single()
    val cases = storedCases(run.id)

    assertEquals(userId, run.userId)
    assertEquals(configurationId, run.problemLanguageId)
    assertEquals("  solution source\n", run.sourceCode)
    assertEquals(CustomInputSubmissionStatus.QUEUED, run.status)
    assertTrue(run.expiresAt >= before.plus(Duration.ofHours(2)))
    assertTrue(run.expiresAt <= Instant.now().plus(Duration.ofHours(2)))
    assertEquals(listOf(0, 1, 2), cases.map { it.position })
    assertEquals(listOf("null", "[1,2]", "9007199254740993"), cases.map { it.inputJson.toString() })
    assertTrue(cases.all { it.expectedOutputJson == null })

    val polled = poll(id)
    assertEquals("QUEUED", polled["status"].asString())
    assertEquals(3, polled["totalCases"].asInt())
    assertEquals(0, polled["passedCases"].asInt())
    assertTrue(polled["outcome"].isNull)
    assertTrue(polled["caseResults"].isNull)
    assertTrue(polled["startedAt"].isNull)
    assertTrue(polled["finishedAt"].isNull)

    val secondId = assertSuccess(enqueueCustomInputSubmission())
    assertNotEquals(id, secondId)
    val runs = storedRuns()
    assertEquals(2, runs.size)
    val secondRun = runs.single { it.id != run.id }
    assertEquals(listOf(JsonPrimitive(1)), storedCases(secondRun.id).map { it.inputJson })
    assertEquals(cases.map { it.id }, storedCases(run.id).map { it.id })
    assertTrue(entClient.problemSubmissions.query {}.all(fixtures).getOrThrow().isEmpty())
  }

  @Test
  fun `invalid source counts and JSON return field errors and roll back every row`() {
    for (inputs in listOf(emptyList(), List(4) { "1" })) {
      assertInvalid(enqueueCustomInputSubmission(inputs), "cases")
    }

    for (source in listOf(" \n", "x".repeat(50_001))) {
      assertInvalid(enqueueCustomInputSubmission(source = source), "sourceCode")
    }

    for (invalid in listOf("", "{", "1 2", "NaN", "01", "{\"a\":1,\"a\":2}", "\"${"x".repeat(19_999)}\"")) {
      assertInvalid(enqueueCustomInputSubmission(listOf("1", invalid)), "cases[1].inputJson")
    }

    assertTrue(storedRuns().isEmpty())
    assertTrue(entClient.customTestCases.query {}.all(fixtures).getOrThrow().isEmpty())
  }

  @Test
  fun `input length applies after compact serialization and source is retained at its limit`() {
    val padded = " ".repeat(20_001) + "null"
    val maximumString = "\"${"x".repeat(19_998)}\""
    assertSuccess(enqueueCustomInputSubmission(listOf(padded, maximumString, "1.0"), source = "x".repeat(50_000)))

    val run = storedRuns().single()
    assertEquals(50_000, run.sourceCode.length)
    assertEquals(JsonNull, storedCases(run.id).first().inputJson)
    assertEquals(20_000, storedCases(run.id)[1].inputJson.toString().length)
    assertEquals("1.0", storedCases(run.id)[2].inputJson.toString())
  }

  @Test
  fun `unavailable targets references and runtime create no attempts`() {
    assertEquals("AuthenticationRequired", enqueueCustomInputSubmission(session = null)["__typename"].asString())

    for (id in listOf("bad!", globalIdUtil.toGlobalId(GraphqlProblemSubmission::class, configurationId),
      globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, Long.MAX_VALUE))) {
      assertEquals("ProblemNotFound", enqueueCustomInputSubmission(configuration = id)["__typename"].asString())
    }

    runtime.available = false
    assertEquals("ExecutionUnavailable", enqueueCustomInputSubmission()["__typename"].asString())
    runtime.available = true

    for (reference in listOf(null, "   ")) {
      entClient.judgeConfigurations.update(judgeId) {
        referenceSolutionCode = reference
      }.save(fixtures).getOrThrow()
      assertEquals("ExecutionUnavailable", enqueueCustomInputSubmission()["__typename"].asString())
    }

    entClient.problems.update(problem.id) { archivedAt = Instant.now() }.save(fixtures).getOrThrow()
    assertEquals("ProblemNotFound", enqueueCustomInputSubmission()["__typename"].asString())
    assertTrue(storedRuns().isEmpty())
  }

  @Test
  fun `polling is owner only and retained results remain readable after archival`() {
    val id = assertSuccess(enqueueCustomInputSubmission())
    val original = poll(id)
    val (_, other) = login()
    val (_, admin) = login(UserRole.ADMIN)

    for (viewer in listOf(null, other, admin)) {
      assertTrue(poll(id, viewer).isNull)
    }

    for (invalid in listOf("bad!", publicConfigurationId,
      globalIdUtil.toGlobalId(GraphqlCustomInputSubmission::class, Long.MAX_VALUE))) {
      assertTrue(poll(invalid).isNull)
    }

    entClient.problems.update(problem.id) { archivedAt = Instant.now() }.save(fixtures).getOrThrow()
    assertEquals(original, poll(id))
  }

  @Test
  fun `finished polling includes every case in input order and distinguishes JSON null`() {
    val id = assertSuccess(enqueueCustomInputSubmission(listOf("null", "2", "3")))
    val run = storedRuns().single()
    val cases = storedCases(run.id)

    entClient.withTransaction { tx ->
      tx.customTestCases.update(cases[0].id) { expectedOutputJson = JsonNull }.save(ExecutionAccess.context).getOrThrow()
      tx.customTestCases.update(cases[1].id) { expectedOutputJson = JsonPrimitive(2) }.save(ExecutionAccess.context).getOrThrow()

      tx.customInputSubmissions.update(run.id) {
        status = CustomInputSubmissionStatus.FINISHED
        outcome = CustomInputSubmissionOutcome.WRONG_ANSWER
        passedCases = 1
        runtimeMs = 12
        startedAt = Instant.now().minusSeconds(1)
        finishedAt = Instant.now()
        caseResults = buildJsonArray {
          addJsonObject {
            put("testCaseId", cases[2].id)
            put("outcome", "NOT_RUN")
            put("output", "")
          }
          addJsonObject {
            put("testCaseId", cases[0].id)
            put("outcome", "PASSED")
            put("output", "null")
          }
          addJsonObject {
            put("testCaseId", cases[1].id)
            put("outcome", "WRONG_ANSWER")
            put("output", "4")
          }
        }
      }.save(ExecutionAccess.context).getOrThrow()
    }.getOrThrow()

    val polled = poll(id)
    assertEquals("FINISHED", polled["status"].asString())
    assertEquals("WRONG_ANSWER", polled["outcome"].asString())
    assertEquals(1, polled["passedCases"].asInt())
    assertEquals(12, polled["runtimeMs"].asInt())
    val caseResults = polled["caseResults"]
    assertEquals(3, caseResults.size())
    val results = (0 until caseResults.size()).map { caseResults[it] }
    assertEquals(listOf(0, 1, 2), results.map { it["testCase"]["position"].asInt() })
    assertEquals(listOf("PASSED", "WRONG_ANSWER", "NOT_RUN"), results.map { it["outcome"].asString() })
    assertEquals(listOf("null", "4", ""), results.map { it["output"].asString() })
    assertEquals("null", results[0]["testCase"]["expectedOutputJson"].asString())
    assertEquals("2", results[1]["testCase"]["expectedOutputJson"].asString())
    assertTrue(results[2]["testCase"]["expectedOutputJson"].isNull)
    assertFalse(polled.toString().contains("private"))
    assertFalse(polled.toString().contains("solution source"))
  }

  @Test
  fun `polling derives case error messages from outcomes instead of stored diagnostics`() {
    val id = assertSuccess(enqueueCustomInputSubmission())
    val run = storedRuns().single()
    val testCase = storedCases(run.id).single()

    entClient.customInputSubmissions.update(run.id) {
      status = CustomInputSubmissionStatus.FINISHED
      outcome = CustomInputSubmissionOutcome.RUNTIME_ERROR
      caseResults = buildJsonArray {
        addJsonObject {
          put("testCaseId", testCase.id)
          put("outcome", "RUNTIME_ERROR")
          put("output", "")
          put("publicErrorMessage", "private reference diagnostic")
        }
      }
    }.save(ExecutionAccess.context).getOrThrow()

    val polled = poll(id)
    assertEquals("The solution stopped with an error.", polled["caseResults"][0]["publicErrorMessage"].asString())
    assertFalse(polled.toString().contains("private reference diagnostic"))
  }

  @Test
  fun `expired retained runs remain readable until cleanup deletes them`() {
    val run = entClient.customInputSubmissions.create {
      userId = this@CustomInputSubmissionIntegrationTest.userId
      problemLanguageId = configurationId
      expiresAt = Instant.now().minusSeconds(1)
      sourceCode = "solution"
      totalCases = 1
    }.saveAndLoad(fixtures).getOrThrow()

    val id = globalIdUtil.toGlobalId(GraphqlCustomInputSubmission::class, run.id)
    assertFalse(poll(id).isNull)
    entClient.customInputSubmissions.deleteById(fixtures, run.id).getOrThrow()
    assertTrue(poll(id).isNull)
  }

  @Test
  fun `EntKt enforces ownership for cases and runs and denies ordinary writes`() {
    assertSuccess(enqueueCustomInputSubmission())
    val run = storedRuns().single()
    val testCase = storedCases(run.id).single()
    val (otherId, other) = login()
    val (adminId, admin) = login(UserRole.ADMIN)

    for ((viewerId, viewerSession) in listOf(userId to session, otherId to other, adminId to admin)) {
      asUser(viewerSession) {
        val viewer = ViewerContext(Viewer.User(viewerId))
        if (viewerId == userId) {
          assertEquals(testCase.id, entClient.customTestCases.findById(viewer, testCase.id).getOrThrow()!!.id)
          assertEquals(run.id, entClient.customInputSubmissions.findById(viewer, run.id).getOrThrow()!!.id)
        } else {
          assertThrows(EntPrivacyDeniedException::class.java) {
            entClient.customTestCases.findById(viewer, testCase.id).getOrThrow()
          }
          assertThrows(EntPrivacyDeniedException::class.java) {
            entClient.customInputSubmissions.findById(viewer, run.id).getOrThrow()
          }
        }

        assertThrows(EntMutationPrivacyDeniedException::class.java) {
          entClient.customInputSubmissions.create {
            userId = viewerId
            problemLanguageId = configurationId
            expiresAt = Instant.now().plusSeconds(60)
            sourceCode = "solution"
            totalCases = 1
          }.save(viewer).getOrThrow()
        }
        assertThrows(EntMutationPrivacyDeniedException::class.java) {
          entClient.customTestCases.update(testCase.id) { expectedOutputJson = JsonNull }.save(viewer).getOrThrow()
        }
        assertThrows(EntMutationPrivacyDeniedException::class.java) {
          entClient.customInputSubmissions.update(run.id) {
            status = CustomInputSubmissionStatus.FINISHED
          }.save(viewer).getOrThrow()
        }
        assertThrows(EntMutationPrivacyDeniedException::class.java) {
          entClient.customInputSubmissions.deleteById(viewer, run.id).getOrThrow()
        }
      }
    }

    asUser(other) {
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.customInputSubmissions.findById(ViewerContext(Viewer.User(userId)), run.id).getOrThrow()
      }
    }
  }

  @Test
  fun `source and case content remain validated for direct execution writes`() {
    assertSuccess(enqueueCustomInputSubmission())
    val run = storedRuns().single()
    val testCase = storedCases(run.id).single()

    val error = assertThrows(EntValidationException::class.java) {
      entClient.customInputSubmissions.create {
        userId = this@CustomInputSubmissionIntegrationTest.userId
        problemLanguageId = configurationId
        expiresAt = Instant.now().plusSeconds(60)
        sourceCode = " "
        totalCases = 1
      }.save(ExecutionAccess.context).getOrThrow()
    }
    assertEquals("sourceCode", error.violations.single().field)

    assertThrows(EntValidationException::class.java) {
      entClient.customTestCases.update(testCase.id) {
        expectedOutputJson = JsonPrimitive("x".repeat(20_000))
      }.save(ExecutionAccess.context).getOrThrow()
    }
  }

  @Test
  fun `official and custom attempts share per-user capacity and finished attempts release it`() {
    createOfficialCase()
    assertEquals("SubmitSolutionSuccess", submitSolution()["__typename"].asString())
    assertSuccess(enqueueCustomInputSubmission())
    assertEquals("ExecutionBusy", enqueueCustomInputSubmission()["__typename"].asString())
    assertEquals("ExecutionBusy", submitSolution()["__typename"].asString())

    val official = entClient.problemSubmissions.query {}.all(fixtures).getOrThrow().single()
    entClient.problemSubmissions.update(official.id) {
      status = ProblemSubmissionStatus.FINISHED
      verdict = ProblemSubmissionVerdict.ACCEPTED
    }.save(ExecutionAccess.context).getOrThrow()
    assertSuccess(enqueueCustomInputSubmission())

    entClient.customInputSubmissions.update(storedRuns().first().id) {
      status = CustomInputSubmissionStatus.FINISHED
      outcome = CustomInputSubmissionOutcome.PASSED
    }.save(ExecutionAccess.context).getOrThrow()
    assertEquals("SubmitSolutionSuccess", submitSolution()["__typename"].asString())
  }

  @Test
  fun `global capacity includes both kinds of attempts from other owners`() {
    createOfficialCase()
    assertSuccess(enqueueCustomInputSubmission())
    assertEquals("SubmitSolutionSuccess", submitSolution()["__typename"].asString())
    val (_, second) = login()
    assertSuccess(enqueueCustomInputSubmission(session = second))
    assertEquals("SubmitSolutionSuccess", submitSolution(second)["__typename"].asString())

    val (_, third) = login()
    assertEquals("ExecutionBusy", enqueueCustomInputSubmission(session = third)["__typename"].asString())
    assertEquals("ExecutionBusy", submitSolution(third)["__typename"].asString())
    assertEquals(2, storedRuns().size)
  }

  @Test
  fun `concurrent official and custom admissions cannot claim the same last user slot`() {
    createOfficialCase()
    assertSuccess(enqueueCustomInputSubmission())
    val executor = Executors.newFixedThreadPool(2)
    val ready = CountDownLatch(2)
    val start = CountDownLatch(1)

    try {
      val requests = listOf<() -> Any>(
        { customInputSubmissionService.enqueueCustomInputSubmission(configurationId, "solution", listOf("1")) },
        { problemSubmissionService.submitSolution(configurationId, "solution") },
      ).map { request ->
        executor.submit<Result<Any>> {
          ready.countDown()
          check(start.await(10, TimeUnit.SECONDS))
          asUser(session) { runCatching(request) }
        }
      }

      assertTrue(ready.await(10, TimeUnit.SECONDS))
      start.countDown()
      val results = requests.map { it.get(30, TimeUnit.SECONDS) }
      assertEquals(1, results.count {
        it.getOrNull() is EnqueueCustomInputSubmissionOutcome.Success || it.getOrNull() is SubmitSolutionOutcome.Success
      })

      for (result in results) {
        val exception = result.exceptionOrNull()
        if (exception != null) {
          assertTrue(generateSequence(exception) { it.cause }.any { it is SQLException && it.sqlState == "40001" })
        } else {
          val outcome = result.getOrThrow()
          assertTrue(outcome is EnqueueCustomInputSubmissionOutcome.Success || outcome is SubmitSolutionOutcome.Success ||
            outcome == EnqueueCustomInputSubmissionOutcome.Busy || outcome == SubmitSolutionOutcome.Busy)
        }
      }

      val officialCount = entClient.problemSubmissions.query {}.all(fixtures).getOrThrow().size
      assertEquals(2, storedRuns().size + officialCount)
      assertEquals(storedRuns().size, entClient.customTestCases.query {}.all(fixtures).getOrThrow().size)
    } finally {
      start.countDown()
      executor.shutdownNow()
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
    }
  }

  private fun storedRuns(): List<CustomInputSubmission> = entClient.customInputSubmissions.query {}
    .all(fixtures).getOrThrow()

  private fun storedCases(customInputSubmissionId: Long): List<CustomTestCase> =
    entClient.customTestCases.indexes.customInputSubmissionId(customInputSubmissionId).query {
      orderBy(CustomTestCase.position.asc())
    }.all(fixtures).getOrThrow()

  private fun createOfficialCase() {
    entClient.testCases.create {
      problemId = problem.id
      position = 0
      visibility = TestCaseVisibility.HIDDEN
      inputJson = JsonPrimitive(1)
      expectedOutputJson = JsonPrimitive(1)
    }.save(fixtures).getOrThrow()
  }

  private fun assertSuccess(result: JsonNode): String {
    assertEquals("EnqueueCustomInputSubmissionSuccess", result["__typename"].asString(), result.toString())
    return result["customInputSubmissionId"].asString()
  }

  private fun assertInvalid(result: JsonNode, field: String) {
    assertEquals("EnqueueCustomInputSubmissionValidationFailure", result["__typename"].asString(), result.toString())
    assertEquals(field, result["fieldErrors"][0]["field"].asString())
  }

  private fun enqueueCustomInputSubmission(
    inputs: List<String> = listOf("1"),
    source: String = "  solution source\n",
    configuration: String = publicConfigurationId,
    session: MockHttpSession? = this.session,
  ): JsonNode = graphql(
    """
      mutation EnqueueCustomInputSubmission(${'$'}input: EnqueueCustomInputSubmissionInput!) {
        enqueueCustomInputSubmission(input: ${'$'}input) {
          __typename
          ... on EnqueueCustomInputSubmissionSuccess { customInputSubmissionId }
          ... on EnqueueCustomInputSubmissionValidationFailure { message fieldErrors { field message } }
        }
      }
    """.trimIndent(),
    mapOf("input" to mapOf(
      "problemLanguageId" to configuration,
      "sourceCode" to source,
      "cases" to inputs.map { mapOf("inputJson" to it) },
    )),
    session,
  )["enqueueCustomInputSubmission"]

  private fun submitSolution(session: MockHttpSession = this.session): JsonNode = graphql(
    """
      mutation Submit(${'$'}input: SubmitSolutionInput!) {
        submitSolution(input: ${'$'}input) { __typename }
      }
    """.trimIndent(),
    mapOf("input" to mapOf("problemLanguageId" to publicConfigurationId, "sourceCode" to "solution")),
    session,
  )["submitSolution"]

  private fun poll(id: String, session: MockHttpSession? = this.session): JsonNode = graphql(
    """
      query Poll(${'$'}id: ID!) {
        customInputSubmission(id: ${'$'}id) {
          id status outcome totalCases passedCases runtimeMs publicErrorMessage createdAt startedAt finishedAt
          caseResults {
            testCase { id position inputJson expectedOutputJson }
            outcome output publicErrorMessage
          }
        }
      }
    """.trimIndent(),
    mapOf("id" to id),
    session,
  )["customInputSubmission"]

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

  private fun login(role: UserRole = UserRole.USER): Pair<Long, MockHttpSession> {
    val email = "${UUID.randomUUID()}@example.com"
    val user = entClient.users.create {
      this.email = email
      hashedPassword = passwordEncoder.encode("test-password")
      this.role = role
    }.saveAndLoad(fixtures).getOrThrow()

    val result = mvc.perform(
      post("/login")
        .cookie(Cookie("XSRF-TOKEN", "test-token"))
        .header("X-XSRF-TOKEN", "test-token")
        .param("username", email)
        .param("password", "test-password"),
    ).andReturn()

    assertEquals("/", result.response.redirectedUrl)
    return user.id to (result.request.session as MockHttpSession)
  }

  private fun <T> asUser(session: MockHttpSession, action: () -> T): T {
    val context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext
    SecurityContextHolder.setContext(context)

    return try {
      action()
    } finally {
      SecurityContextHolder.clearContext()
    }
  }

  class TestRuntimeAvailability : RuntimeAvailability {
    var available = true
    override fun isAvailable(runtime: String): Boolean = available
  }

  @TestConfiguration(proxyBeanMethods = false)
  class RuntimeConfiguration {
    @Bean
    @Primary
    fun runtimeAvailability() = TestRuntimeAvailability()
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

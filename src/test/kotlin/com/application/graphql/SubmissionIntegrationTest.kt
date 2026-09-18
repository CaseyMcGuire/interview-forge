package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Problem
import com.application.ent.Submission
import com.application.ent.SubmissionTestResult
import com.application.ent.TestCase
import com.application.execution.RuntimeAvailability
import com.application.schema.ProblemDifficulty
import com.application.schema.ProblemCheckerKind
import com.application.schema.SubmissionKind
import com.application.schema.SubmissionStatus
import com.application.schema.SubmissionVerdict
import com.application.security.ExecutionAccess
import com.application.services.SubmissionService
import com.application.services.SubmitSolutionOutcome
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import entkt.runtime.result.EntValidationException
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
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage
import com.application.graphql.types.Submission as GraphqlSubmission

@Testcontainers
@SpringBootTest(
  properties = [
    "execution.runtimes.kotlin=test-runtime",
    "execution.max-active-submissions=4",
    "execution.max-active-submissions-per-user=2",
  ],
)
@Import(SubmissionIntegrationTest.SubmissionTestConfiguration::class)
class SubmissionIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect isolated submission test fixtures",
  )

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var submissionService: SubmissionService

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
  @Qualifier("springSecurityFilterChain")
  lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc
  private lateinit var session: MockHttpSession
  private var userId = 0L
  private lateinit var problem: Problem
  private var configurationId = 0L
  private var judgeId = 0L

  private val publicConfigurationId get() = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configurationId)

  @BeforeEach
  fun setUp() {
    entClient.withTransaction { tx ->
      tx.submissionTestResults.deleteMany(fixtureContext).getOrThrow()
      tx.submissions.deleteMany(fixtureContext).getOrThrow()
    }.getOrThrow()

    runtime.available = true

    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()

    val login = login(UserRole.USER)
    userId = login.first
    session = login.second

    problem = entClient.problems.create {
      slug = "submission-${UUID.randomUUID()}"
      title = "Submission fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
      publishedAt = Instant.now().minusSeconds(60)
    }.saveAndLoad(fixtureContext).getOrThrow()

    val language = entClient.languages.indexes.key("kotlin").find(fixtureContext).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = problem.id
      languageId = language.id
      starterCode = "fun solve() = 1"
    }.saveAndLoad(fixtureContext).getOrThrow().id

    judgeId = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      testDriverCode = "private driver"
      timeLimitMs = 1000
      memoryLimitMb = 256
    }.saveAndLoad(fixtureContext).getOrThrow().id
  }

  @Test
  fun `official submissions retain source and queue without copying test inputs`() {
    createCase(7, TestCaseVisibility.EXAMPLE, "7")
    createCase(3, TestCaseVisibility.HIDDEN, "\"secret\"")

    val result = submitSolution()
    assertSuccess(result)

    val submission = result["submission"]
    assertEquals("QUEUED", submission["status"].asString())
    assertTrue(submission["verdict"].isNull)
    assertTrue(submission["startedAt"].isNull)
    assertTrue(submission["finishedAt"].isNull)
    assertEquals(0, submission["totalCases"].asInt())
    assertEquals(0, submission["passedCases"].asInt())
    assertFalse(submission.has("testResults"))
    assertFalse(submission.toString().contains("secret"))
    assertFalse(submission.toString().contains("private driver"))

    val stored = storedSubmissions().single()
    assertEquals(userId, stored.userId)
    assertEquals(SubmissionKind.SUBMIT, stored.kind)
    assertEquals(configurationId, stored.problemLanguageId)
    assertEquals("  solution source\n", stored.sourceCode)
    assertTrue(storedCases().isEmpty())
    assertEquals(submission, poll(submission["id"].asString()))
  }

  @Test
  fun `official suites may contain only hidden tests and are not limited to twenty cases`() {
    repeat(21) { position ->
      createCase(position, TestCaseVisibility.HIDDEN, position.toString())
    }

    assertSuccess(submitSolution())
    assertTrue(storedCases().isEmpty())
  }

  @Test
  fun `the API has no example run mutation or custom case input`() {
    val runResponse = graphql(
      "mutation { runSubmission(input: {problemLanguageId: \"unused\", sourceCode: \"solution\"}) { __typename } }",
      emptyMap(),
      session,
      allowErrors = true,
    )
    assertTrue(runResponse.has("errors"), runResponse.toString())

    val customCaseResponse = graphql(
      "mutation Submit(${'$'}input: SubmitSolutionInput!) { submitSolution(input: ${'$'}input) { __typename } }",
      mapOf(
        "input" to mapOf(
          "problemLanguageId" to publicConfigurationId,
          "sourceCode" to "solution",
          "customCases" to listOf(mapOf("inputJson" to "1")),
        ),
      ),
      session,
      allowErrors = true,
    )
    assertTrue(customCaseResponse.has("errors"), customCaseResponse.toString())
    assertTrue(storedSubmissions().isEmpty())
    assertTrue(storedCases().isEmpty())
  }

  @Test
  fun `invalid source returns field errors without creating a submission`() {
    createCase(0, TestCaseVisibility.HIDDEN, "1")

    for (sourceCode in listOf(" \n", "x".repeat(50_001))) {
      val result = submitSolution(mapOf("sourceCode" to sourceCode))

      assertEquals("SubmissionValidationFailure", result["__typename"].asString(), result.toString())
      assertEquals("sourceCode", result["fieldErrors"][0]["field"].asString())
      assertTrue(storedSubmissions().isEmpty())
    }

    assertSuccess(submitSolution(mapOf("sourceCode" to "x".repeat(50_000))))
  }

  @Test
  fun `source validation also applies to entity creation outside the service`() {
    for (sourceCode in listOf(" \n", "x".repeat(50_001))) {
      val exception = assertThrows(EntValidationException::class.java) {
        createSubmissionDirectly(sourceCode)
      }

      assertEquals("sourceCode", exception.violations.single().field)
      assertTrue(storedSubmissions().isEmpty())
    }

    val sourceCode = "x".repeat(50_000)
    assertEquals(sourceCode, createSubmissionDirectly(sourceCode).sourceCode)
  }

  @Test
  fun `only the owner can poll official submissions even after archival`() {
    createCase(0, TestCaseVisibility.HIDDEN, "1")
    val submission = submitSolution()["submission"]
    val id = submission["id"].asString()
    val (_, other) = login(UserRole.USER)
    val (_, admin) = login(UserRole.ADMIN)

    for (viewer in listOf(null, other, admin)) {
      assertTrue(poll(id, viewer).isNull)
    }

    for (invalidId in listOf(
      "invalid!",
      publicConfigurationId,
      globalIdUtil.toGlobalId(GraphqlSubmission::class, Long.MAX_VALUE),
    )) {
      assertTrue(poll(invalidId).isNull)
    }

    entClient.problems.update(problem.id) {
      archivedAt = Instant.now()
    }.save(fixtureContext).getOrThrow()
    assertEquals(submission, poll(id))

    val legacyRun = createSubmissionDirectly("legacy source", SubmissionKind.RUN)
    val legacyRunId = globalIdUtil.toGlobalId(GraphqlSubmission::class, legacyRun.id)
    assertTrue(poll(legacyRunId).isNull)
  }

  @Test
  fun `polling reflects persisted official execution progress and verdicts`() {
    createCase(0, TestCaseVisibility.HIDDEN, "1")
    val publicId = submitSolution()["submission"]["id"].asString()
    val stored = storedSubmissions().single()
    val started = Instant.now()

    entClient.submissions.update(stored.id) {
      status = SubmissionStatus.RUNNING
      totalCases = 3
      passedCases = 1
      startedAt = started
    }.save(ExecutionAccess.context).getOrThrow()

    val running = poll(publicId)
    assertEquals("RUNNING", running["status"].asString())
    assertEquals(3, running["totalCases"].asInt())
    assertEquals(1, running["passedCases"].asInt())
    assertTrue(running["verdict"].isNull)

    entClient.submissions.update(stored.id) {
      status = SubmissionStatus.FINISHED
      verdict = SubmissionVerdict.ACCEPTED
      passedCases = 3
      runtimeMs = 12
      peakMemoryMb = 64
      finishedAt = started.plusSeconds(1)
    }.save(ExecutionAccess.context).getOrThrow()

    val finished = poll(publicId)
    assertEquals("FINISHED", finished["status"].asString())
    assertEquals("ACCEPTED", finished["verdict"].asString())
    assertEquals(3, finished["passedCases"].asInt())
    assertEquals(12, finished["runtimeMs"].asInt())
    assertEquals(64, finished["peakMemoryMb"].asInt())
    assertFalse(finished["finishedAt"].isNull)
  }

  @Test
  fun `authentication missing targets and unavailable execution return expected failures`() {
    assertEquals("AuthenticationRequired", submitSolution(session = null)["__typename"].asString())

    for (id in listOf(
      "bad!",
      globalIdUtil.toGlobalId(GraphqlSubmission::class, configurationId),
      globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, Long.MAX_VALUE),
    )) {
      val result = submitSolution(mapOf("problemLanguageId" to id))
      assertEquals("ProblemNotFound", result["__typename"].asString())
    }

    assertEquals("ExecutionUnavailable", submitSolution()["__typename"].asString())

    createCase(0, TestCaseVisibility.HIDDEN, "1")
    runtime.available = false
    assertEquals("ExecutionUnavailable", submitSolution()["__typename"].asString())

    runtime.available = true
    entClient.problems.update(problem.id) {
      archivedAt = Instant.now()
    }.save(fixtureContext).getOrThrow()

    assertEquals("ProblemNotFound", submitSolution()["__typename"].asString())
    assertTrue(storedSubmissions().isEmpty())
  }

  @Test
  fun `concurrent requests cannot exceed the per-user submission limit`() {
    createCase(0, TestCaseVisibility.HIDDEN, "1")
    assertSuccess(submitSolution())

    val executor = Executors.newFixedThreadPool(4)
    val ready = CountDownLatch(4)
    val start = CountDownLatch(1)

    try {
      val requests = (0 until 4).map {
        executor.submit<Result<SubmitSolutionOutcome>> {
          ready.countDown()
          check(start.await(10, TimeUnit.SECONDS))
          submitSolutionAsUser(session)
        }
      }

      assertTrue(ready.await(10, TimeUnit.SECONDS))
      start.countDown()

      val outcomes = requests.map { it.get(30, TimeUnit.SECONDS) }
      assertSingleAcceptedSubmission(outcomes)
      assertEquals(2, storedSubmissions().size)
      assertTrue(storedCases().isEmpty())
      assertEquals("ExecutionBusy", submitSolution()["__typename"].asString())
    } finally {
      start.countDown()
      executor.shutdownNow()
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
    }
  }

  @Test
  fun `the last global queue slot can be accepted only once across different users`() {
    createCase(0, TestCaseVisibility.HIDDEN, "1")
    assertSuccess(submitSolution())
    assertSuccess(submitSolution())

    val (_, second) = login(UserRole.USER)
    assertSuccess(submitSolution(session = second))

    val contenders = listOf(login(UserRole.USER).second, login(UserRole.USER).second)
    val executor = Executors.newFixedThreadPool(2)
    val ready = CountDownLatch(2)
    val start = CountDownLatch(1)

    try {
      val requests = contenders.map { viewer ->
        executor.submit<Result<SubmitSolutionOutcome>> {
          ready.countDown()
          check(start.await(10, TimeUnit.SECONDS))
          submitSolutionAsUser(viewer)
        }
      }

      assertTrue(ready.await(10, TimeUnit.SECONDS))
      start.countDown()

      val outcomes = requests.map { it.get(30, TimeUnit.SECONDS) }
      assertSingleAcceptedSubmission(outcomes)
      assertEquals(4, storedSubmissions().size)
      assertEquals("ExecutionBusy", submitSolution(session = contenders.first())["__typename"].asString())
    } finally {
      start.countDown()
      executor.shutdownNow()
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
    }
  }

  @Test
  fun `privacy permits official test reads only for execution and denies ordinary lifecycle writes`() {
    val hidden = createCase(0, TestCaseVisibility.HIDDEN, "\"hidden\"")
    assertSuccess(submitSolution())
    val submission = storedSubmissions().single()
    val (otherId, _) = login(UserRole.USER)
    val currentContext = session.getAttribute(
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
    ) as SecurityContext
    SecurityContextHolder.setContext(currentContext)

    try {
      val owner = ViewerContext(Viewer.User(userId))
      assertEquals(submission.id, entClient.submissions.findById(owner, submission.id).getOrThrow()!!.id)

      for (viewer in listOf(Viewer.Anonymous, Viewer.User(otherId))) {
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.submissions.findById(ViewerContext(viewer), submission.id).getOrThrow()
        }
      }

      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.submissions.update(submission.id) {
          status = SubmissionStatus.FINISHED
        }.save(owner).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.submissions.deleteById(owner, submission.id).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.submissions.create {
          userId = this@SubmissionIntegrationTest.userId
          problemId = problem.id
          problemLanguageId = configurationId
          sourceCode = "forged"
          kind = SubmissionKind.SUBMIT
          totalCases = 0
        }.save(owner).getOrThrow()
      }

      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.judgeConfigurations.findById(owner, judgeId).getOrThrow()
      }
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.testCases.findById(owner, hidden.id).getOrThrow()
      }

      assertEquals(
        judgeId,
        entClient.judgeConfigurations.findById(ExecutionAccess.context, judgeId).getOrThrow()!!.id,
      )
      assertEquals(
        hidden.id,
        entClient.testCases.findById(ExecutionAccess.context, hidden.id).getOrThrow()!!.id,
      )

      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.users.findById(ExecutionAccess.context, userId).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.testCases.update(hidden.id) {
          inputJson = JsonPrimitive("forged")
        }.save(ExecutionAccess.context).getOrThrow()
      }
    } finally {
      SecurityContextHolder.clearContext()
    }
  }

  @Test
  fun `unsupported checkers missing judges and disabled languages cannot be queued`() {
    createCase(0, TestCaseVisibility.HIDDEN, "1")
    entClient.problems.update(problem.id) {
      checkerKind = ProblemCheckerKind.CUSTOM
    }.save(fixtureContext).getOrThrow()
    assertEquals("ExecutionUnavailable", submitSolution()["__typename"].asString())

    entClient.problems.update(problem.id) {
      checkerKind = ProblemCheckerKind.EXACT_JSON
    }.save(fixtureContext).getOrThrow()

    val languageId = entClient.problemLanguages.findById(fixtureContext, configurationId)
      .getOrThrow()!!
      .languageId
    entClient.languages.update(languageId) { enabled = false }.save(fixtureContext).getOrThrow()

    try {
      assertEquals("ProblemNotFound", submitSolution()["__typename"].asString())
    } finally {
      entClient.languages.update(languageId) { enabled = true }.save(fixtureContext).getOrThrow()
    }

    entClient.judgeConfigurations.deleteById(fixtureContext, judgeId).getOrThrow()
    assertEquals("ExecutionUnavailable", submitSolution()["__typename"].asString())
    assertTrue(storedSubmissions().isEmpty())
  }

  private fun assertSuccess(result: JsonNode) {
    assertEquals("SubmitSolutionSuccess", result["__typename"].asString(), result.toString())
  }

  private fun submitSolutionAsUser(session: MockHttpSession): Result<SubmitSolutionOutcome> {
    val context = session.getAttribute(
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
    ) as SecurityContext
    SecurityContextHolder.setContext(context)

    return try {
      runCatching { submissionService.submitSolution(configurationId, "solution source") }
    } finally {
      SecurityContextHolder.clearContext()
    }
  }

  private fun assertSingleAcceptedSubmission(outcomes: List<Result<SubmitSolutionOutcome>>) {
    assertEquals(1, outcomes.count { it.getOrNull() is SubmitSolutionOutcome.Success }, outcomes.toString())

    for (outcome in outcomes) {
      val exception = outcome.exceptionOrNull()
      if (exception != null) {
        // Without retries, a competing transaction can abort instead of observing the full queue.
        assertTrue(
          generateSequence(exception) { it.cause }.any { it is SQLException && it.sqlState == "40001" },
          exception.toString(),
        )
      } else {
        val value = outcome.getOrThrow()
        assertTrue(value is SubmitSolutionOutcome.Success || value == SubmitSolutionOutcome.Busy)
      }
    }
  }

  private fun storedSubmissions(): List<Submission> = entClient.submissions.query {}
    .all(fixtureContext)
    .getOrThrow()

  private fun createSubmissionDirectly(
    sourceCode: String,
    kind: SubmissionKind = SubmissionKind.SUBMIT,
  ): Submission =
    entClient.submissions.create {
      userId = this@SubmissionIntegrationTest.userId
      problemId = problem.id
      problemLanguageId = configurationId
      this.sourceCode = sourceCode
      this.kind = kind
      totalCases = 1
    }
      .saveAndLoad(ExecutionAccess.context)
      .getOrThrow()

  private fun storedCases(): List<SubmissionTestResult> = entClient.submissionTestResults.query {
    orderBy(SubmissionTestResult.position.asc())
  }
    .all(fixtureContext)
    .getOrThrow()

  private fun createCase(
    position: Int,
    visibility: TestCaseVisibility,
    input: String,
  ): TestCase =
    entClient.testCases.create {
      problemId = problem.id
      this.position = position
      this.visibility = visibility
      inputJson = Json.parseToJsonElement(input)
      expectedOutputJson = JsonNull
    }
      .saveAndLoad(fixtureContext)
      .getOrThrow()

  private fun login(role: UserRole): Pair<Long, MockHttpSession> {
    val email = "${UUID.randomUUID()}@example.com"
    val user = entClient.users.create {
      this.email = email
      hashedPassword = passwordEncoder.encode("test-password")
      this.role = role
    }.saveAndLoad(fixtureContext).getOrThrow()

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

  private fun submitSolution(
    fields: Map<String, Any?> = emptyMap(),
    session: MockHttpSession? = this.session,
  ): JsonNode = graphql(
    """
      mutation Submit(${'$'}input: SubmitSolutionInput!) {
        submitSolution(input: ${'$'}input) {
          __typename
          ... on SubmitSolutionSuccess { submission { $SUBMISSION_FIELDS } }
          ... on SubmissionValidationFailure { message fieldErrors { field message } }
          ... on AuthenticationRequired { message }
          ... on ProblemNotFound { message }
          ... on ExecutionUnavailable { message }
          ... on ExecutionBusy { message }
        }
      }
    """.trimIndent(),
    mapOf(
      "input" to (mapOf(
        "problemLanguageId" to publicConfigurationId,
        "sourceCode" to "  solution source\n",
      ) + fields),
    ),
    session,
  )["submitSolution"]

  private fun poll(id: String, session: MockHttpSession? = this.session): JsonNode = graphql(
    "query Poll(${'$'}id: ID!) { submission(id: ${'$'}id) { $SUBMISSION_FIELDS } }",
    mapOf("id" to id),
    session,
  )["submission"]

  private fun graphql(
    query: String,
    variables: Map<String, Any?>,
    session: MockHttpSession?,
    allowErrors: Boolean = false,
  ): JsonNode {
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
    if (allowErrors) {
      return response
    }

    assertFalse(response.has("errors"), response.toString())
    return response["data"]
  }

  class TestRuntimeAvailability : RuntimeAvailability {
    @Volatile
    var available = true

    override fun isAvailable(runtime: String): Boolean {
      return available
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  class SubmissionTestConfiguration {
    @Bean
    @Primary
    fun runtimeAvailability() = TestRuntimeAvailability()
  }

  companion object {
    private const val SUBMISSION_FIELDS = """
      id status verdict totalCases passedCases runtimeMs peakMemoryMb publicErrorMessage
      createdAt startedAt finishedAt
    """

    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

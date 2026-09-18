package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.TestCase
import com.application.graphql.GlobalIdUtil
import com.application.schema.ProblemDifficulty
import com.application.schema.SubmissionKind
import com.application.schema.SubmissionStatus
import com.application.schema.SubmissionVerdict
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.security.ExecutionAccess
import com.application.services.SubmissionService
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntPrivacyDeniedException
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
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
import java.nio.file.Files
import java.util.UUID
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage

@Testcontainers
@SpringBootTest(properties = ["execution.runtimes.kotlin=" + DockerExecutionServiceTest.IMAGE])
@Import(SubmissionExecutionIntegrationTest.ExecutionTestConfiguration::class)
class SubmissionExecutionIntegrationTest {
  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var dockerExecutor: DockerExecutionService

  @Autowired
  lateinit var submissionService: SubmissionService

  private lateinit var scheduler: SubmissionScheduler
  private lateinit var runner: SubmissionRunner
  private lateinit var executor: FakeProgramExecutor

  @Autowired
  lateinit var context: WebApplicationContext

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  lateinit var mapper: ObjectMapper

  @Autowired
  lateinit var globalIdUtil: GlobalIdUtil

  @Autowired
  @Qualifier("springSecurityFilterChain")
  lateinit var securityFilter: Filter

  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated submission execution test fixtures")
  private lateinit var mvc: MockMvc
  private lateinit var session: MockHttpSession
  private var userId = 0L
  private var problemId = 0L
  private var configurationId = 0L
  private var judgeId = 0L

  @BeforeEach
  fun setUp() {
    executor = FakeProgramExecutor()
    runner = SubmissionRunner(submissionService, executor)
    scheduler = createScheduler(executor)
    scheduler.onApplicationReady()

    entClient.withTransaction { tx ->
      tx.submissionTestResults.deleteMany(fixtures).getOrThrow()
      tx.submissions.deleteMany(fixtures).getOrThrow()
    }.getOrThrow()

    mvc = MockMvcBuilders.webAppContextSetup(context)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()
    session = signIn()
    problemId = entClient.problems.create {
      slug = "execution-${UUID.randomUUID()}"
      title = "Execution fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
      publishedAt = Instant.now().minusSeconds(60)
    }.saveAndLoad(fixtures).getOrThrow().id

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = this@SubmissionExecutionIntegrationTest.problemId
      languageId = language.id
      starterCode = "fun solve(input: String) = input"
    }.saveAndLoad(fixtures).getOrThrow().id
    judgeId = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      testDriverCode = "fun main() { print(solve(System.`in`.bufferedReader().readText())) }"
      timeLimitMs = 1_000
      memoryLimitMb = 256
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `the runner executes the claimed submission and leaves final persistence to the service`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val firstId = submit("first solution")
    val secondId = submit("second solution")
    val submission = submissionService.claimNextQueuedSubmission()!!
    assertEquals("first solution", submission.sourceCode)
    assertEquals(SubmissionStatus.RUNNING, submission.status)
    assertNotNull(submission.startedAt)
    executor.runSuite = { _, _, _ -> TestSuiteResult(TestSuiteStatus.WRONG_ANSWER, 0, 0, stdout = "2") }

    val result = runner.runSubmission(submission)

    assertEquals(SubmissionVerdict.WRONG_ANSWER, result.verdict)
    assertEquals("RUNNING", poll(firstId)["status"].asString())
    assertEquals("QUEUED", poll(secondId)["status"].asString())
    assertTrue(storedResults().isEmpty())
    assertEquals(listOf("prepare", "compile", "suite", "close"), executor.events)

    submissionService.finishSubmission(submission.id, result)

    assertEquals("FINISHED", poll(firstId)["status"].asString())
    assertEquals("WRONG_ANSWER", poll(firstId)["verdict"].asString())
    assertEquals("2", storedResults().single().stdout)
    assertEquals("QUEUED", poll(secondId)["status"].asString())
  }

  @Test
  fun `the runner compiles once and passes the current ordered suite in one call`() {
    createCase(7, TestCaseVisibility.HIDDEN, 7)
    createCase(1, TestCaseVisibility.EXAMPLE, 1)
    val id = submit("submitted source")

    createCase(4, TestCaseVisibility.HIDDEN, 4)
    entClient.judgeConfigurations.update(judgeId) {
      testDriverCode = "updated driver"
      timeLimitMs = 1_234
      memoryLimitMb = 192
    }.save(fixtures).getOrThrow()

    executor.runSuite = { cases, timeLimitMs, memoryLimitMb ->
      assertEquals(listOf(1, 4, 7).map(::JsonPrimitive), cases.map { it.input })
      assertEquals(cases.map { it.input }, cases.map { it.expectedOutput })
      assertEquals(1_234, timeLimitMs)
      assertEquals(192, memoryLimitMb)

      val running = poll(id)
      assertEquals("RUNNING", running["status"].asString())
      assertEquals(3, running["totalCases"].asInt())
      assertEquals(0, running["passedCases"].asInt())
      assertTrue(storedResults().isEmpty())
      TestSuiteResult(TestSuiteStatus.PASSED, 3, runtimeMs = 42)
    }

    scheduler.processQueuedSubmissions()
    scheduler.processQueuedSubmissions()
    assertEquals(listOf("cleanup", "prepare", "compile", "suite", "close"), executor.events)
    assertEquals(entClient.submissions.query {}.all(fixtures).getOrThrow().single().id, executor.submissionId)
    assertEquals(DockerExecutionServiceTest.IMAGE, executor.runtime)
    assertEquals("submitted source", executor.program.sourceFiles["Solution.kt"])
    assertEquals("updated driver", executor.program.sourceFiles["TestDriver.kt"])

    val result = poll(id)
    assertEquals("FINISHED", result["status"].asString())
    assertEquals("ACCEPTED", result["verdict"].asString())
    assertEquals(3, result["passedCases"].asInt())
    assertEquals(42, result["runtimeMs"].asInt())
    assertFalse(result["startedAt"].isNull)
    assertFalse(result["finishedAt"].isNull)
    assertTrue(storedResults().isEmpty())
  }

  @Test
  fun `only the first failed case is retained using its selected inputs and visibility`() {
    createCase(1, TestCaseVisibility.EXAMPLE, 1)
    val hidden = createCase(5, TestCaseVisibility.HIDDEN, 2)
    createCase(9, TestCaseVisibility.EXAMPLE, 3)
    val id = submit("solution")

    executor.runSuite = { _, _, _ ->
      entClient.testCases.update(hidden.id) {
        visibility = TestCaseVisibility.EXAMPLE
        inputJson = JsonPrimitive(99)
        expectedOutputJson = JsonPrimitive(99)
      }.save(fixtures).getOrThrow()

      TestSuiteResult(
        TestSuiteStatus.WRONG_ANSWER, passedCases = 1, failedCaseIndex = 1,
        stdout = "0", stderr = "private diagnostic", runtimeMs = 17,
      )
    }
    scheduler.processQueuedSubmissions()

    val result = poll(id)
    assertEquals("WRONG_ANSWER", result["verdict"].asString())
    assertEquals(1, result["passedCases"].asInt())
    assertEquals(3, result["totalCases"].asInt())
    assertFalse(result.toString().contains("private diagnostic"))

    val retained = storedResults().single()
    assertEquals(5, retained.position)
    assertEquals("HIDDEN", retained.source.name)
    assertEquals(JsonPrimitive(2), retained.inputJson)
    assertEquals(JsonPrimitive(2), retained.expectedOutputJson)
    assertEquals("0", retained.stdout)
    assertNull(retained.runtimeMs, "Suite duration cannot be attributed to the failed case")
    assertEquals(
      retained.id,
      entClient.submissionTestResults.findById(ExecutionAccess.context, retained.id).getOrThrow()!!.id,
    )

    val ownerId = userId
    val admin = signIn(UserRole.ADMIN)
    assertTrue(poll(id, admin).isNull)
    for (viewer in listOf(Viewer.Anonymous, Viewer.User(ownerId), Viewer.User(userId))) {
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.submissionTestResults.findById(ViewerContext(viewer), retained.id).getOrThrow()
      }
    }
  }

  @Test
  fun `compilation failures close the program without running or retaining cases`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("invalid solution")
    executor.compilation = ProgramResult(ProgramStatus.FAILED, stderr = "TestDriver private diagnostic")

    scheduler.processQueuedSubmissions()

    val result = poll(id)
    assertEquals("FINISHED", result["status"].asString())
    assertEquals("COMPILE_ERROR", result["verdict"].asString())
    assertFalse(result.toString().contains("TestDriver"))
    assertEquals(listOf("cleanup", "prepare", "compile", "close"), executor.events)
    assertTrue(storedResults().isEmpty())
  }

  @Test
  fun `suite failures map to public verdicts and sanitize retained diagnostics`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)

    for ((status, verdict) in listOf(
      TestSuiteStatus.INVALID_OUTPUT to "RUNTIME_ERROR",
      TestSuiteStatus.RUNTIME_ERROR to "RUNTIME_ERROR",
      TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED to "RUNTIME_ERROR",
      TestSuiteStatus.TIME_LIMIT_EXCEEDED to "TIME_LIMIT_EXCEEDED",
      TestSuiteStatus.MEMORY_LIMIT_EXCEEDED to "MEMORY_LIMIT_EXCEEDED",
    )) {
      entClient.submissionTestResults.deleteMany(fixtures).getOrThrow()
      val id = submit("solution")
      executor.runSuite = { _, _, _ ->
        TestSuiteResult(status, 0, 0, stdout = "\u0000" + "x".repeat(20_001), stderr = "private diagnostic")
      }

      scheduler.processQueuedSubmissions()

      val result = poll(id)
      assertEquals("FINISHED", result["status"].asString())
      assertEquals(verdict, result["verdict"].asString())
      assertFalse(result.toString().contains("private diagnostic"))
      val retained = storedResults().single()
      assertEquals(verdict, retained.outcome.name)
      assertEquals("\uFFFD" + "x".repeat(19_999), retained.stdout)
    }
  }

  @Test
  fun `a JVM failure before the first case has no failed-case record`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("solution")
    executor.runSuite = { _, _, _ -> TestSuiteResult(TestSuiteStatus.MEMORY_LIMIT_EXCEEDED, 0) }

    scheduler.processQueuedSubmissions()

    assertEquals("MEMORY_LIMIT_EXCEEDED", poll(id)["verdict"].asString())
    assertTrue(storedResults().isEmpty())
  }

  @Test
  fun `executor exceptions close the program and finish with a safe infrastructure error`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("solution")
    executor.runSuite = { _, _, _ -> error("secret driver input") }

    scheduler.processQueuedSubmissions()

    val result = poll(id)
    assertEquals("FINISHED", result["status"].asString())
    assertEquals("INTERNAL_ERROR", result["verdict"].asString())
    assertFalse(result.toString().contains("secret"))
    assertEquals("close", executor.events.last())
    assertTrue(storedResults().isEmpty())
  }

  @Test
  fun `interruption finishes the attempt closes the program and propagates to the caller`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("solution")
    executor.runSuite = { _, _, _ -> throw InterruptedException("stopping") }

    try {
      assertThrows(InterruptedException::class.java) { scheduler.processQueuedSubmissions() }
      assertTrue(Thread.interrupted(), "The scheduler must restore the interruption flag")
      assertEquals("INTERNAL_ERROR", poll(id)["verdict"].asString())
      assertEquals("close", executor.events.last())
      assertTrue(storedResults().isEmpty())
    } finally {
      Thread.interrupted()
    }
  }

  @Test
  fun `interrupted attempts are finished before queued work executes`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val interrupted = entClient.submissions.create {
      userId = this@SubmissionExecutionIntegrationTest.userId
      problemId = this@SubmissionExecutionIntegrationTest.problemId
      problemLanguageId = configurationId
      sourceCode = "previous attempt"
      kind = SubmissionKind.SUBMIT
      status = SubmissionStatus.RUNNING
      totalCases = 2
      passedCases = 1
      runtimeMs = 12
      startedAt = Instant.now().minusSeconds(60)
    }.saveAndLoad(fixtures).getOrThrow()
    val queuedId = submit("solution")

    scheduler.processQueuedSubmissions()

    val recovered = entClient.submissions.findById(fixtures, interrupted.id).getOrThrow()!!
    assertEquals(SubmissionStatus.FINISHED, recovered.status)
    assertEquals(SubmissionVerdict.INTERNAL_ERROR, recovered.verdict)
    assertEquals(1, recovered.passedCases)
    assertEquals(12L, recovered.runtimeMs)
    assertEquals("ACCEPTED", poll(queuedId)["verdict"].asString())
  }

  @Test
  fun `failed container cleanup leaves interrupted submissions running until recovery succeeds`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("interrupted solution")
    val submission = entClient.submissions.query {}.all(fixtures).getOrThrow().single()
    entClient.submissions.update(submission.id) {
      status = SubmissionStatus.RUNNING
      startedAt = Instant.now()
    }.save(fixtures).getOrThrow()
    executor.cleanupFailure = IllegalStateException("Docker is unavailable")

    scheduler.processQueuedSubmissions()
    assertEquals("RUNNING", poll(id)["status"].asString())
    assertEquals(listOf("cleanup"), executor.events)

    executor.cleanupFailure = null
    scheduler.processQueuedSubmissions()
    assertEquals("INTERNAL_ERROR", poll(id)["verdict"].asString())
    assertEquals(listOf("cleanup", "cleanup"), executor.events)
  }

  @Test
  fun `scheduler executes a queued submission through Docker and publishes its summary`() {
    createCase(4, TestCaseVisibility.HIDDEN, 4)
    createCase(1, TestCaseVisibility.EXAMPLE, 1)
    val id = submit("fun solve(input: String) = input")
    val scheduler = createScheduler(dockerExecutor)

    scheduler.processQueuedSubmissions()
    assertEquals("QUEUED", poll(id)["status"].asString(), "Wait for application startup")

    scheduler.onApplicationReady()
    scheduler.processQueuedSubmissions()

    val result = poll(id)
    assertEquals("FINISHED", result["status"].asString())
    assertEquals("ACCEPTED", result["verdict"].asString())
    assertEquals(2, result["passedCases"].asInt())
    assertTrue(result["runtimeMs"].asLong() > 0)
    assertTrue(storedResults().isEmpty())
    assertRuntimeWorkspaceEmpty()
  }

  @Test
  fun `Docker suite preserves state and persists only its first failure`() {
    createCase(1, TestCaseVisibility.EXAMPLE, 1)
    createCase(5, TestCaseVisibility.HIDDEN, 1)
    createCase(9, TestCaseVisibility.HIDDEN, 99)
    val id = submit("""
      var calls = 0
      fun solve(input: String): String {
        if (input == "99") while (true) {}
        return (input.toInt() + calls++).toString()
      }
    """.trimIndent())

    val scheduler = createScheduler(dockerExecutor)
    scheduler.onApplicationReady()
    scheduler.processQueuedSubmissions()

    val result = poll(id)
    assertEquals("WRONG_ANSWER", result["verdict"].asString())
    assertEquals(1, result["passedCases"].asInt())
    assertEquals(3, result["totalCases"].asInt())
    val failed = storedResults().single()
    assertEquals(5, failed.position)
    assertEquals("HIDDEN", failed.source.name)
    assertEquals("2", failed.stdout)
    assertRuntimeWorkspaceEmpty()
  }

  @Test
  fun `Docker compilation failures finish without a test result`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("fun solve(input: String) = invalid syntax")

    val scheduler = createScheduler(dockerExecutor)
    scheduler.onApplicationReady()
    scheduler.processQueuedSubmissions()

    assertEquals("COMPILE_ERROR", poll(id)["verdict"].asString())
    assertTrue(storedResults().isEmpty())
    assertRuntimeWorkspaceEmpty()
  }

  @Test
  fun `missing settings finish the submission before preparing a program`() {
    createCase(0, TestCaseVisibility.HIDDEN, 1)
    val id = submit("solution")
    entClient.judgeConfigurations.deleteById(fixtures, judgeId).getOrThrow()

    scheduler.processQueuedSubmissions()

    assertEquals("INTERNAL_ERROR", poll(id)["verdict"].asString())
    assertEquals(listOf("cleanup"), executor.events)
    assertTrue(storedResults().isEmpty())
  }

  @Test
  fun `legacy example runs are not claimed`() {
    val legacy = entClient.submissions.create {
      userId = this@SubmissionExecutionIntegrationTest.userId
      problemId = this@SubmissionExecutionIntegrationTest.problemId
      problemLanguageId = configurationId
      sourceCode = "legacy run"
      kind = SubmissionKind.RUN
      totalCases = 0
    }.saveAndLoad(fixtures).getOrThrow()

    scheduler.processQueuedSubmissions()
    assertEquals(
      SubmissionStatus.QUEUED,
      entClient.submissions.findById(fixtures, legacy.id).getOrThrow()!!.status,
    )
    assertEquals(listOf("cleanup"), executor.events)
  }

  private fun createCase(position: Int, visibility: TestCaseVisibility, value: Int): TestCase =
    entClient.testCases.create {
      problemId = this@SubmissionExecutionIntegrationTest.problemId
      this.position = position
      this.visibility = visibility
      inputJson = JsonPrimitive(value)
      expectedOutputJson = JsonPrimitive(value)
    }.saveAndLoad(fixtures).getOrThrow()

  private fun storedResults() = entClient.submissionTestResults.query {}.all(fixtures).getOrThrow()

  private fun createScheduler(executor: ProgramExecutor) = SubmissionScheduler(
    submissionService,
    SubmissionRunner(submissionService, executor),
    executor,
    ExecutionProperties(runtimes = mapOf("kotlin" to DockerExecutionServiceTest.IMAGE)),
  )

  private fun assertRuntimeWorkspaceEmpty() {
    Files.list(runtimeWorkspace).use { assertEquals(0, it.count()) }
  }

  private fun signIn(role: UserRole = UserRole.USER): MockHttpSession {
    val email = "${UUID.randomUUID()}@example.com"
    userId = entClient.users.create {
      this.email = email
      hashedPassword = passwordEncoder.encode("test-password")
      this.role = role
    }.saveAndLoad(fixtures).getOrThrow().id

    val result = mvc.perform(
      post("/login")
        .cookie(Cookie("XSRF-TOKEN", "token"))
        .header("X-XSRF-TOKEN", "token")
        .param("username", email)
        .param("password", "test-password"),
    ).andReturn()
    assertEquals("/", result.response.redirectedUrl)
    return result.request.session as MockHttpSession
  }

  private fun submit(source: String): String {
    val response = graphql(
      """mutation(${'$'}input: SubmitSolutionInput!) {
        submitSolution(input: ${'$'}input) { __typename ... on SubmitSolutionSuccess { submission { id } } }
      }""".trimIndent(),
      mapOf("input" to mapOf(
        "problemLanguageId" to globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configurationId),
        "sourceCode" to source,
      )),
      session,
    )["submitSolution"]
    assertEquals("SubmitSolutionSuccess", response["__typename"].asString(), response.toString())
    return response["submission"]["id"].asString()
  }

  private fun poll(id: String, viewer: MockHttpSession = session): JsonNode = graphql(
    """query(${'$'}id: ID!) { submission(id: ${'$'}id) {
      status verdict totalCases passedCases runtimeMs startedAt finishedAt publicErrorMessage
    } }""".trimIndent(),
    mapOf("id" to id),
    viewer,
  )["submission"]

  private fun graphql(query: String, variables: Map<String, Any>, viewer: MockHttpSession): JsonNode {
    var result = mvc.perform(
      post("/graphql")
        .session(viewer)
        .contentType("application/json")
        .cookie(Cookie("XSRF-TOKEN", "token"))
        .header("X-XSRF-TOKEN", "token")
        .content(mapper.writeValueAsString(mapOf("query" to query, "variables" to variables))),
    ).andReturn()

    if (result.request.isAsyncStarted) {
      result = mvc.perform(asyncDispatch(result)).andReturn()
    }

    assertEquals(200, result.response.status)
    val response = mapper.readTree(result.response.contentAsString)
    assertFalse(response.has("errors"), response.toString())
    return response["data"]
  }

  /** Keeps the review focused on how the runner uses the execution contract. */
  private class FakeProgramExecutor : ProgramExecutor {
    val events = mutableListOf<String>()
    var submissionId = 0L
    lateinit var runtime: String
    lateinit var program: PreparedProgram
    var cleanupFailure: RuntimeException? = null
    var compilation = ProgramResult(ProgramStatus.SUCCEEDED)
    var runSuite: (List<TestCaseInput>, Int, Int) -> TestSuiteResult = { cases, _, _ ->
      TestSuiteResult(TestSuiteStatus.PASSED, cases.size)
    }

    override fun isAvailable(runtime: String) = true

    override fun cleanUpInterruptedExecutions() {
      events += "cleanup"
      cleanupFailure?.let { throw it }
    }

    override fun prepareProgram(submissionId: Long, runtime: String, program: PreparedProgram): ProgramExecution {
      events += "prepare"
      this.submissionId = submissionId
      this.runtime = runtime
      this.program = program

      return object : ProgramExecution {
        override fun compileProgram(): ProgramResult {
          events += "compile"
          return compilation
        }

        override fun runTestSuite(cases: List<TestCaseInput>, timeLimitMs: Int, memoryLimitMb: Int): TestSuiteResult {
          events += "suite"
          return runSuite(cases, timeLimitMs, memoryLimitMb)
        }

        override fun close() {
          events += "close"
        }
      }
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  class ExecutionTestConfiguration {
    @Bean
    @Primary
    fun runtimeAvailability() = RuntimeAvailability { true }
  }

  companion object {
    private val runtimeWorkspace = Files.createTempDirectory("submission-execution-runtime-")

    @JvmStatic
    @DynamicPropertySource
    fun runtimeProperties(registry: DynamicPropertyRegistry) {
      registry.add("execution.workspace-directory") { runtimeWorkspace.toString() }
    }

    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

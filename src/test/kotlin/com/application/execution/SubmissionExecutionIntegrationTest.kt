package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.Submission
import com.application.ent.TestCase
import com.application.graphql.GlobalIdUtil
import com.application.schema.ProblemDifficulty
import com.application.schema.SubmissionStatus
import com.application.schema.SubmissionTestOutcome
import com.application.schema.SubmissionVerdict
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.security.ExecutionAccess
import com.application.services.SubmissionService
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.JsonNull
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
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
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
import java.sql.SQLException
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
      tx.submissionFailures.deleteMany(fixtures).getOrThrow()
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

    val withoutFailure = entClient.submissions.query {
      where(Submission.id eq submission.id)
      loadFailedTestResult()
    }.firstOrNull(ExecutionAccess.context).getOrThrow()!!
    assertNull(withoutFailure.edges.failedTestResult.requireLoaded())

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

    val withFailure = entClient.submissions.query {
      where(Submission.id eq submission.id)
      loadFailedTestResult()
    }.firstOrNull(ExecutionAccess.context).getOrThrow()!!
    assertEquals(storedResults().single().id, withFailure.edges.failedTestResult.requireLoaded()!!.id)
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
      assertTrue(running["failedExample"].isNull)
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
    assertTrue(result["failedExample"].isNull)
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
    assertTrue(result["failedExample"].isNull)
    assertFalse(result.toString().contains("private diagnostic"))

    val retained = storedResults().single()
    assertEquals("HIDDEN", retained.source.name)
    assertEquals(JsonPrimitive(2), retained.inputJson)
    assertEquals(JsonPrimitive(2), retained.expectedOutputJson)
    assertEquals("0", retained.stdout)
    assertNull(retained.runtimeMs, "Suite duration cannot be attributed to the failed case")
    assertEquals(
      retained.id,
      entClient.submissionFailures.findById(ExecutionAccess.context, retained.id).getOrThrow()!!.id,
    )

    val ownerId = userId
    val admin = signIn(UserRole.ADMIN)
    assertTrue(poll(id, admin).isNull)
    for (viewer in listOf(Viewer.Anonymous, Viewer.User(ownerId), Viewer.User(userId))) {
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.submissionFailures.findById(ViewerContext(viewer), retained.id).getOrThrow()
      }
    }
  }

  @Test
  fun `the owner can poll a failed example after its input visibility and problem change`() {
    val example = createCase(4, TestCaseVisibility.EXAMPLE, 4)
    val id = submit("solution")
    assertTrue(poll(id)["failedExample"].isNull)
    executor.runSuite = { _, _, _ ->
      TestSuiteResult(TestSuiteStatus.WRONG_ANSWER, 0, 0, stdout = "0", stderr = "private driver diagnostic")
    }

    scheduler.processQueuedSubmissions()

    entClient.testCases.update(example.id) {
      inputJson = JsonPrimitive(99)
      expectedOutputJson = JsonPrimitive(99)
      visibility = TestCaseVisibility.HIDDEN
    }.save(fixtures).getOrThrow()
    entClient.problems.update(problemId) { archivedAt = Instant.now() }.save(fixtures).getOrThrow()

    val submission = poll(id)
    val failed = submission["failedExample"]
    assertEquals("4", failed["inputJson"].asString())
    assertEquals("4", failed["expectedOutputJson"].asString())
    assertEquals("0", failed["output"].asString())
    assertFalse(submission.toString().contains("private driver diagnostic"))
    assertEquals(1, storedResults().size)

    assertTrue(poll(id, null).isNull)
    assertTrue(poll(id, signIn()).isNull)
    assertTrue(poll(id, signIn(UserRole.ADMIN)).isNull)
  }

  @Test
  fun `failed examples preserve JSON null and output that is not valid JSON`() {
    val example = createCase(0, TestCaseVisibility.EXAMPLE, 1)
    entClient.testCases.update(example.id) {
      inputJson = JsonNull
      expectedOutputJson = JsonNull
    }.save(fixtures).getOrThrow()
    val id = submit("solution")
    executor.runSuite = { _, _, _ -> TestSuiteResult(TestSuiteStatus.INVALID_OUTPUT, 0, 0, stdout = "not JSON") }

    scheduler.processQueuedSubmissions()

    val submission = poll(id)
    assertEquals("RUNTIME_ERROR", submission["verdict"].asString())
    val failed = submission["failedExample"]
    assertEquals("null", failed["inputJson"].asString())
    assertEquals("null", failed["expectedOutputJson"].asString())
    assertEquals("not JSON", failed["output"].asString())
  }

  @Test
  fun `failed-example privacy requires the authenticated owner and denies result mutations`() {
    createCase(0, TestCaseVisibility.EXAMPLE, 1)
    submit("solution")
    executor.runSuite = { _, _, _ -> TestSuiteResult(TestSuiteStatus.WRONG_ANSWER, 0, 0, stdout = "0") }
    scheduler.processQueuedSubmissions()

    val result = storedResults().single()
    val ownerId = userId
    val owner = ViewerContext(Viewer.User(ownerId))
    val stranger = signIn()
    val strangerId = userId
    val admin = signIn(UserRole.ADMIN)
    val adminId = userId

    try {
      authenticate(session)
      assertEquals(result.id, entClient.submissionFailures.findById(owner, result.id).getOrThrow()!!.id)

      for (viewer in listOf(Viewer.Anonymous, Viewer.User(strangerId), Viewer.User(adminId))) {
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.submissionFailures.findById(ViewerContext(viewer), result.id).getOrThrow()
        }
      }

      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.submissionFailures.update(result.id) { stdout = "forged" }.save(owner).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.submissionFailures.deleteById(owner, result.id).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.submissionFailures.create {
          submissionId = result.submissionId
          source = result.source
          inputJson = result.inputJson
          expectedOutputJson = result.expectedOutputJson
          outcome = result.outcome
        }.save(owner).getOrThrow()
      }

      for ((login, viewerId) in listOf(stranger to strangerId, admin to adminId)) {
        authenticate(login)
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.submissionFailures.findById(ViewerContext(Viewer.User(viewerId)), result.id).getOrThrow()
        }
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.submissionFailures.findById(owner, result.id).getOrThrow()
        }
      }

      SecurityContextHolder.clearContext()
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.submissionFailures.findById(owner, result.id).getOrThrow()
      }
    } finally {
      SecurityContextHolder.clearContext()
    }
  }

  @Test
  fun `unfinished attempts and non-failing case records have no failed example`() {
    createCase(0, TestCaseVisibility.EXAMPLE, 1)
    val id = submit("solution")
    executor.runSuite = { _, _, _ -> TestSuiteResult(TestSuiteStatus.WRONG_ANSWER, 0, 0, stdout = "0") }
    scheduler.processQueuedSubmissions()
    val result = storedResults().single()

    for (status in listOf(SubmissionStatus.QUEUED, SubmissionStatus.RUNNING)) {
      entClient.submissions.update(result.submissionId) { this.status = status }.save(fixtures).getOrThrow()
      assertTrue(poll(id)["failedExample"].isNull)
    }

    entClient.submissions.update(result.submissionId) {
      status = SubmissionStatus.FINISHED
    }.save(fixtures).getOrThrow()

    for (outcome in listOf(
      SubmissionTestOutcome.PENDING,
      SubmissionTestOutcome.PASSED,
      SubmissionTestOutcome.EXECUTED,
      SubmissionTestOutcome.SKIPPED,
    )) {
      entClient.submissionFailures.update(result.id) { this.outcome = outcome }.save(fixtures).getOrThrow()
      assertTrue(poll(id)["failedExample"].isNull)
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
    assertTrue(result["failedExample"].isNull)
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
      entClient.submissionFailures.deleteMany(fixtures).getOrThrow()
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
  fun `a submission cannot retain a second failure`() {
    createCase(0, TestCaseVisibility.EXAMPLE, 1)
    submit("first solution")
    submit("second solution")
    executor.runSuite = { _, _, _ -> TestSuiteResult(TestSuiteStatus.WRONG_ANSWER, 0, 0, stdout = "0") }
    scheduler.processQueuedSubmissions()
    val retained = storedResults().single()

    val exception = assertThrows(Exception::class.java) {
      entClient.submissionFailures.create {
        submissionId = retained.submissionId
        source = retained.source
        inputJson = JsonPrimitive("another input")
        expectedOutputJson = retained.expectedOutputJson
        outcome = retained.outcome
      }.save(ExecutionAccess.context).getOrThrow()
    }
    assertTrue(
      generateSequence<Throwable>(exception) { it.cause }.any { it is SQLException && it.sqlState == "23505" },
      exception.toString(),
    )
    assertEquals(retained.id, storedResults().single().id)

    scheduler.processQueuedSubmissions()

    val failures = storedResults()
    assertEquals(2, failures.size)
    assertEquals(2, failures.map { it.submissionId }.distinct().size)
  }

  private fun createCase(position: Int, visibility: TestCaseVisibility, value: Int): TestCase =
    entClient.testCases.create {
      problemId = this@SubmissionExecutionIntegrationTest.problemId
      this.position = position
      this.visibility = visibility
      inputJson = JsonPrimitive(value)
      expectedOutputJson = JsonPrimitive(value)
    }.saveAndLoad(fixtures).getOrThrow()

  private fun storedResults() = entClient.submissionFailures.query {}.all(fixtures).getOrThrow()

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

  private fun poll(id: String, viewer: MockHttpSession? = session): JsonNode = graphql(
    """query(${'$'}id: ID!) { submission(id: ${'$'}id) {
      status verdict totalCases passedCases runtimeMs startedAt finishedAt publicErrorMessage
      failedExample { inputJson expectedOutputJson output }
    } }""".trimIndent(),
    mapOf("id" to id),
    viewer,
  )["submission"]

  private fun graphql(query: String, variables: Map<String, Any>, viewer: MockHttpSession?): JsonNode {
    val request = post("/graphql")
      .contentType("application/json")
      .cookie(Cookie("XSRF-TOKEN", "token"))
      .header("X-XSRF-TOKEN", "token")
      .content(mapper.writeValueAsString(mapOf("query" to query, "variables" to variables)))
    if (viewer != null) {
      request.session(viewer)
    }

    var result = mvc.perform(request).andReturn()

    if (result.request.isAsyncStarted) {
      result = mvc.perform(asyncDispatch(result)).andReturn()
    }

    assertEquals(200, result.response.status)
    val response = mapper.readTree(result.response.contentAsString)
    assertFalse(response.has("errors"), response.toString())
    return response["data"]
  }

  private fun authenticate(session: MockHttpSession) {
    val securityContext = session.getAttribute(
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
    ) as SecurityContext
    SecurityContextHolder.setContext(securityContext)
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

package com.application.execution

import com.application.config.ExecutionProperties
import com.application.db.policies.GradingJobPolicy
import com.application.db.policies.CustomTestCasePolicy
import com.application.db.policies.CustomInputSubmissionPolicy
import com.application.security.CurrentUserService
import com.application.services.ExecutionAvailabilityService
import com.application.ent.EntClient
import com.application.ent.CustomTestCase
import com.application.ent.CustomInputSubmission
import com.application.graphql.GlobalIdUtil
import com.application.schema.ProblemDifficulty
import com.application.schema.GradingJobStatus
import com.application.schema.CustomInputSubmissionStatus
import com.application.schema.GradingCase
import com.application.schema.ProblemSubmissionVerdict
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.services.GradingJobService
import com.application.services.CustomInputSubmissionService
import com.application.services.CodeExecutionSettingsService
import com.application.services.ProblemSubmissionService
import entkt.postgres.PostgresDriver
import entkt.runtime.privacy.ViewerContext
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.availability.ApplicationAvailability
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.IntervalTask
import javax.sql.DataSource
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage

@Testcontainers
@SpringBootTest(properties = ["execution.runtimes.kotlin=" + DockerExecutionServiceTest.IMAGE])
@Import(CustomInputSubmissionExecutionIntegrationTest.ExecutionTestConfiguration::class)
class CustomInputSubmissionExecutionIntegrationTest {
  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var dockerExecutor: DockerExecutionService

  @Autowired
  lateinit var gradingJobService: GradingJobService

  @Autowired
  lateinit var customInputSubmissionService: CustomInputSubmissionService

  @Autowired
  lateinit var settingsService: CodeExecutionSettingsService

  @Autowired
  lateinit var problemSubmissionService: ProblemSubmissionService

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var applicationAvailability: ApplicationAvailability

  private lateinit var gradingScheduler: GradingScheduler
  private lateinit var customScheduler: CustomInputSubmissionScheduler
  private lateinit var executor: TestCodeExecutionService

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
    executor = TestCodeExecutionService()
    configureSchedulers(executor)

    entClient.withTransaction { tx ->
      tx.customInputSubmissions.deleteMany(fixtures).getOrThrow()
      tx.problemSubmissionFailures.deleteMany(fixtures).getOrThrow()
      tx.problemSubmissions.deleteMany(fixtures).getOrThrow()
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
      problemId = this@CustomInputSubmissionExecutionIntegrationTest.problemId
      languageId = language.id
      starterCode = "fun solve(input: String) = input"
    }.saveAndLoad(fixtures).getOrThrow().id
    judgeId = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      testDriverCode = "fun main() { print(solve(System.`in`.bufferedReader().readText())) }"
      referenceSolutionCode = "fun solve(input: String) = input"
      timeLimitMs = 1_000
      memoryLimitMb = 256
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `custom inputs are prepared asynchronously and every submitted result is retained`() {
    val id = enqueue(listOf("null", "1.0", "2"))
    assertTrue(executor.calls.isEmpty())
    assertEquals("QUEUED", poll(id)["status"].asString())
    assertTrue(poll(id)["caseResults"].isNull)
    assertTrue(storedJobs().isEmpty())

    executor.reference = { inputs ->
      assertEquals("RUNNING", poll(id)["status"].asString())
      assertTrue(poll(id)["caseResults"].isNull)
      assertTrue(storedJobs().isEmpty())
      outputs(inputs)
    }
    executor.submitted = { inputs ->
      val job = storedJobs().single()
      assertEquals("user source", job.sourceCode)
      assertNull(job.problemSubmissionId)
      assertEquals(GradingJobStatus.RUNNING, job.status)
      assertEquals(inputs, cases(job.customInputSubmissionId!!).map { it.expectedOutputJson })
      outputs(inputs, listOf(JsonNull, JsonPrimitive(1), JsonPrimitive(2)))
    }

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    val result = poll(id)
    assertEquals("FINISHED", result["status"].asString())
    assertEquals("WRONG_ANSWER", result["outcome"].asString())
    assertEquals(2, result["passedCases"].asInt())
    assertEquals(42, result["runtimeMs"].asInt())
    assertEquals(listOf("PASSED", "WRONG_ANSWER", "PASSED"), caseResults(result).map { it["outcome"].asString() })
    assertEquals(listOf("null", "1.0", "2"), caseResults(result).map { it["testCase"]["expectedOutputJson"].asString() })
    assertEquals(listOf("null", "1", "2"), caseResults(result).map { it["output"].asString() })
    assertEquals(listOf("reference", "submitted"), executor.calls)
    assertEquals(listOf("fun solve(input: String) = input", "user source"), executor.sources)
    assertTrue(storedJobs().isEmpty())
    assertTrue(entClient.problemSubmissions.query().all(fixtures).getOrThrow().isEmpty())

    entClient.problems.update(problemId) { archivedAt = Instant.now() }.save(fixtures).getOrThrow()
    assertEquals(result, poll(id))
    assertTrue(poll(id, null).isNull)
    assertTrue(poll(id, signIn()).isNull)
    assertTrue(poll(id, signIn(UserRole.ADMIN)).isNull)
  }

  @ParameterizedTest
  @ValueSource(strings = ["compile", "runtime", "timeout", "memory", "overflow", "invalid", "incomplete", "oversized"])
  fun `reference failures leave all expectations absent and never execute submitted code`(failure: String) {
    val id = enqueue()
    executor.reference = { inputs ->
      when (failure) {
        "compile" -> CodeExecutionResult.CompilationFailed
        "runtime" -> CodeExecutionResult.Completed(ProgramStatus.FAILED, emptyList())
        "timeout" -> CodeExecutionResult.Completed(ProgramStatus.TIME_LIMIT_EXCEEDED, emptyList())
        "memory" -> CodeExecutionResult.Completed(ProgramStatus.MEMORY_LIMIT_EXCEEDED, emptyList())
        "overflow" -> CodeExecutionResult.Completed(ProgramStatus.OUTPUT_LIMIT_EXCEEDED, emptyList())
        "incomplete" -> CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, emptyList())
        "oversized" -> outputs(inputs, inputs.map { JsonPrimitive("x".repeat(20_001)) })
        else -> CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, inputs.map {
          TestCaseExecutionResult(it, ProgramStatus.SUCCEEDED, stdout = "private invalid answer")
        })
      }
    }

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    val result = poll(id)
    assertEquals("REFERENCE_SOLUTION_FAILED", result["outcome"].asString())
    assertEquals(3, result["caseResults"].size())
    assertEquals(listOf("reference"), executor.calls)
    assertTrue(result["caseResults"].all { it["outcome"].asString() == "NOT_RUN" && it["output"].asString().isEmpty() })
    assertTrue(result["caseResults"].all { it["testCase"]["expectedOutputJson"].isNull })
    assertFalse(result.toString().contains("private"))
    assertTrue(storedJobs().isEmpty())
  }

  @Test
  fun `submitted compilation failure retains expectations but no executed cases`() {
    val id = enqueue()
    executor.submitted = { CodeExecutionResult.CompilationFailed }

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    val result = poll(id)
    assertEquals("COMPILE_ERROR", result["outcome"].asString())
    assertEquals(listOf("1", "2", "3"), caseResults(result).map { it["testCase"]["expectedOutputJson"].asString() })
    assertTrue(result["caseResults"].all { it["outcome"].asString() == "NOT_RUN" })
    assertEquals(listOf("reference", "submitted"), executor.calls)
    assertTrue(storedJobs().isEmpty())
  }

  @Test
  fun `submitted timeout preserves completed cases and marks remaining inputs NOT_RUN`() {
    val id = enqueue()
    executor.submitted = { inputs ->
      CodeExecutionResult.Completed(ProgramStatus.TIME_LIMIT_EXCEEDED, listOf(
        TestCaseExecutionResult(inputs[0], ProgramStatus.SUCCEEDED, inputs[0], "1"),
        TestCaseExecutionResult(inputs[1], ProgramStatus.TIME_LIMIT_EXCEEDED, stderr = "private driver detail"),
      ))
    }

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    val result = poll(id)
    assertEquals("TIME_LIMIT_EXCEEDED", result["outcome"].asString())
    assertEquals(1, result["passedCases"].asInt())
    assertEquals(listOf("PASSED", "TIME_LIMIT_EXCEEDED", "NOT_RUN"), caseResults(result).map { it["outcome"].asString() })
    assertFalse(result.toString().contains("private"))
    assertTrue(storedJobs().isEmpty())
  }

  @Test
  fun `claiming skips locked custom submissions and leaves them queued until their locks are released`() {
    val firstId = enqueue(source = "first solution")
    val firstDatabaseId = globalIdUtil.fromGlobalIdOrNull(firstId)!!
    val secondId = enqueue(source = "second solution")

    Executors.newSingleThreadExecutor().use { worker ->
      entClient.withTransaction { tx ->
        val lockedSubmission = tx.customInputSubmissions.query { where(CustomInputSubmission.id eq firstDatabaseId) }
          .forUpdate()
          .firstOrNull(fixtures)
          .getOrThrow()!!

        val claimed = worker.submit<CustomInputSubmission?> {
          customInputSubmissionService.claimNextQueuedCustomInputSubmission()
        }.get(5, TimeUnit.SECONDS)!!

        assertEquals("second solution", claimed.sourceCode)
        assertEquals(CustomInputSubmissionStatus.RUNNING, claimed.status)
        assertNotNull(claimed.startedAt)

        val nextSubmission = worker.submit<CustomInputSubmission?> {
          customInputSubmissionService.claimNextQueuedCustomInputSubmission()
        }.get(5, TimeUnit.SECONDS)

        assertNull(nextSubmission)
        val stillLocked = tx.customInputSubmissions.findById(fixtures, lockedSubmission.id).getOrThrow()!!
        assertEquals(CustomInputSubmissionStatus.QUEUED, stillLocked.status)
        assertNull(stillLocked.startedAt)
      }.getOrThrow()
    }

    assertEquals("QUEUED", poll(firstId)["status"].asString())
    assertEquals("RUNNING", poll(secondId)["status"].asString())
    assertEquals(firstDatabaseId, customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!.id)
    assertEquals("RUNNING", poll(firstId)["status"].asString())
  }

  @Test
  fun `recovery finishes interrupted preparations and preserves ready custom jobs`() {
    val interruptedId = enqueue()
    customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!
    val readyId = enqueue(listOf("null", "1"))
    val ready = customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!
    customInputSubmissionService.saveExpectedOutputsAndEnqueueGradingJob(ready.id, listOf(JsonNull, JsonPrimitive(1)))

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    assertEquals("INTERNAL_ERROR", poll(interruptedId)["outcome"].asString())
    assertTrue(poll(interruptedId)["caseResults"].all { it["testCase"]["expectedOutputJson"].isNull })
    assertEquals("PASSED", poll(readyId)["outcome"].asString())
    assertEquals(ready.startedAt, storedCustomInputSubmission(ready.id).startedAt)
    assertEquals(listOf("submitted"), executor.calls)
    assertTrue(storedJobs().isEmpty())
  }

  @Test
  fun `failed Docker recovery leaves running custom jobs untouched until cleanup succeeds`() {
    val id = enqueue()
    val run = customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!
    customInputSubmissionService.saveExpectedOutputsAndEnqueueGradingJob(run.id, cases(run.id).map { it.inputJson })
    gradingJobService.claimNextQueuedGradingJob()!!
    executor.cleanupFailure = IllegalStateException("Docker unavailable")

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()
    assertEquals("RUNNING", poll(id)["status"].asString())
    assertEquals(GradingJobStatus.RUNNING, storedJobs().single().status)

    executor.cleanupFailure = null
    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    assertEquals("INTERNAL_ERROR", poll(id)["outcome"].asString())
    assertTrue(poll(id)["caseResults"].all { it["outcome"].asString() == "NOT_RUN" })
    assertTrue(storedJobs().isEmpty())
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `reference interruption finishes the run and propagates with the interrupt flag restored`() {
    val id = enqueue()
    executor.reference = { throw InterruptedException("shutdown") }

    try {
      assertThrows(InterruptedException::class.java) { customScheduler.runScheduledTask() }
      assertTrue(Thread.interrupted())
      assertEquals("INTERNAL_ERROR", poll(id)["outcome"].asString())
      assertTrue(storedJobs().isEmpty())
      assertEquals(listOf("reference"), executor.calls)
    } finally {
      Thread.interrupted()
    }
  }

  @Test
  fun `missing reference configuration finishes with a safe infrastructure error`() {
    val id = enqueue()
    entClient.judgeConfigurations.update(judgeId) { referenceSolutionCode = null }.save(fixtures).getOrThrow()

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    assertEquals("INTERNAL_ERROR", poll(id)["outcome"].asString())
    assertTrue(executor.calls.isEmpty())
    assertTrue(storedJobs().isEmpty())
  }

  @Test
  fun `a failed job insert rolls back every prepared answer`() {
    val id = enqueue()
    val run = customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!
    var attemptedInsert = false
    val failingClient = EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies {
        customInputSubmissions(context.getBean(CustomInputSubmissionPolicy::class.java))
        customTestCases(context.getBean(CustomTestCasePolicy::class.java))
        gradingJobs(context.getBean(GradingJobPolicy::class.java))
      }
      hooks {
        gradingJobs {
          beforeCreate {
            attemptedInsert = true
            error("Simulated job insertion failure")
          }
        }
      }
    }
    val failingService = CustomInputSubmissionService(
      failingClient,
      context.getBean(CurrentUserService::class.java),
      ExecutionProperties(),
      context.getBean(ExecutionAvailabilityService::class.java),
    )

    val failure = assertThrows(Exception::class.java) {
      failingService.saveExpectedOutputsAndEnqueueGradingJob(run.id, cases(run.id).map { it.inputJson })
    }

    assertTrue(attemptedInsert, failure.toString())
    assertTrue(cases(run.id).all { it.expectedOutputJson == null })
    assertTrue(storedJobs().isEmpty())
    assertEquals(CustomInputSubmissionStatus.RUNNING, storedCustomInputSubmission(run.id).status)
    assertNull(storedCustomInputSubmission(run.id).caseResults)

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()
    assertEquals("INTERNAL_ERROR", poll(id)["outcome"].asString())
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `a failed custom job deletion rolls back the retained results`() {
    val id = enqueue()
    val run = customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!
    customInputSubmissionService.saveExpectedOutputsAndEnqueueGradingJob(run.id, cases(run.id).map { it.inputJson })
    val job = gradingJobService.claimNextQueuedGradingJob()!!
    val settings = settingsService.loadSubmittedCodeSettings(job.problemLanguageId, job.sourceCode)
    val result = CodeGrader(executor).gradeCode(
      "test-job", settings.runtime, settings.program,
      job.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
      settings.timeLimitMs, settings.memoryLimitMb,
    )
    var attemptedDelete = false
    val failingClient = EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies {
        customInputSubmissions(context.getBean(CustomInputSubmissionPolicy::class.java))
        customTestCases(context.getBean(CustomTestCasePolicy::class.java))
        gradingJobs(context.getBean(GradingJobPolicy::class.java))
      }
      hooks {
        gradingJobs {
          beforeDelete {
            attemptedDelete = true
            error("Simulated job deletion failure")
          }
        }
      }
    }
    val failingService = GradingJobService(failingClient, problemSubmissionService, customInputSubmissionService)

    val failure = assertThrows(Exception::class.java) { failingService.finishGradingJob(job.id, result) }

    assertTrue(attemptedDelete, failure.toString())
    assertEquals("RUNNING", poll(id)["status"].asString())
    assertTrue(poll(id)["caseResults"].isNull)
    assertEquals(GradingJobStatus.RUNNING, storedJobs().single().status)

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()
    assertEquals("INTERNAL_ERROR", poll(id)["outcome"].asString())
    assertTrue(storedJobs().isEmpty())
    assertEquals(listOf("submitted"), executor.calls)
  }

  @Test
  fun `official and custom jobs share the queue without repeating reference preparation`() {
    val official = entClient.problemSubmissions.create {
      userId = this@CustomInputSubmissionExecutionIntegrationTest.userId
      problemId = this@CustomInputSubmissionExecutionIntegrationTest.problemId
      problemLanguageId = configurationId
      sourceCode = "official source"
      totalCases = 1
    }.saveAndLoad(fixtures).getOrThrow()
    entClient.gradingJobs.create {
      problemSubmissionId = official.id
      problemLanguageId = configurationId
      sourceCode = official.sourceCode
      cases = listOf(GradingCase(1, JsonPrimitive(1), JsonPrimitive(1), TestCaseVisibility.HIDDEN))
    }.save(fixtures).getOrThrow()
    val id = enqueue()

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    assertEquals(ProblemSubmissionVerdict.ACCEPTED, entClient.problemSubmissions.findById(fixtures, official.id).getOrThrow()!!.verdict)
    assertEquals("RUNNING", poll(id)["status"].asString())
    assertEquals(GradingJobStatus.QUEUED, storedJobs().single().status)
    assertEquals(listOf("reference", "submitted"), executor.calls)

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    assertEquals("PASSED", poll(id)["outcome"].asString())
    assertEquals(listOf("reference", "submitted", "submitted"), executor.calls)
    assertEquals(listOf("fun solve(input: String) = input", "official source", "user source"), executor.sources)
    assertTrue(storedJobs().isEmpty())
  }

  @Test
  fun `Docker prepares reference answers then grades all custom inputs`() {
    val id = enqueue(source = "fun solve(input: String) = if (input == \"2\") \"0\" else input")
    configureSchedulers(dockerExecutor)

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    val result = poll(id)
    assertEquals("WRONG_ANSWER", result["outcome"].asString())
    assertEquals(2, result["passedCases"].asInt())
    assertEquals(listOf("1", "2", "3"), caseResults(result).map { it["testCase"]["expectedOutputJson"].asString() })
    assertEquals(listOf("PASSED", "WRONG_ANSWER", "PASSED"), caseResults(result).map { it["outcome"].asString() })
    assertEquals(listOf("1", "0", "3"), caseResults(result).map { it["output"].asString() })
    assertTrue(storedJobs().isEmpty())
    Files.list(runtimeWorkspace).use { assertEquals(0, it.count()) }
  }

  @Test
  fun `Spring registers preparation grading and cleanup at their own intervals`() {
    val tasks = context.getBean(ScheduledAnnotationBeanPostProcessor::class.java).scheduledTasks
    val intervals = tasks.map { (it.task as IntervalTask).intervalDuration.toMillis() }.sorted()

    assertEquals(listOf(1_000L, 1_000L, 60_000L), intervals)
  }

  @Test
  fun `grading progresses while reference preparation is still running`() {
    assertEquals(3, (context.getBean(TaskScheduler::class.java) as ThreadPoolTaskScheduler).scheduledThreadPoolExecutor.corePoolSize)
    val readyId = enqueue()
    val ready = customInputSubmissionService.claimNextQueuedCustomInputSubmission()!!
    customInputSubmissionService.saveExpectedOutputsAndEnqueueGradingJob(ready.id, cases(ready.id).map { it.inputJson })
    val preparingId = enqueue()
    val referenceStarted = CountDownLatch(1)
    val releaseReference = CountDownLatch(1)
    executor.reference = { inputs ->
      referenceStarted.countDown()
      check(releaseReference.await(10, TimeUnit.SECONDS)) { "Reference execution was not released" }
      outputs(inputs)
    }

    Executors.newSingleThreadExecutor().use { worker ->
      val preparation = worker.submit { customScheduler.runScheduledTask() }
      try {
        assertTrue(referenceStarted.await(10, TimeUnit.SECONDS))

        gradingScheduler.runScheduledTask()

        assertEquals("PASSED", poll(readyId)["outcome"].asString())
        assertEquals("RUNNING", poll(preparingId)["status"].asString())
        assertTrue(poll(preparingId)["caseResults"].isNull)
      } finally {
        releaseReference.countDown()
      }
      preparation.get(10, TimeUnit.SECONDS)
    }

    gradingScheduler.runScheduledTask()
    assertEquals("PASSED", poll(preparingId)["outcome"].asString())
    assertEquals(listOf("reference", "submitted", "submitted"), executor.calls)
  }

  @Test
  fun `custom preparation progresses while another run is being graded`() {
    val gradingId = enqueue()
    customScheduler.runScheduledTask()
    val preparingId = enqueue()
    val gradingStarted = CountDownLatch(1)
    val releaseGrading = CountDownLatch(1)
    executor.submitted = { inputs ->
      gradingStarted.countDown()
      check(releaseGrading.await(10, TimeUnit.SECONDS)) { "Grading was not released" }
      outputs(inputs)
    }

    Executors.newSingleThreadExecutor().use { worker ->
      val grading = worker.submit { gradingScheduler.runScheduledTask() }
      try {
        assertTrue(gradingStarted.await(10, TimeUnit.SECONDS))

        customScheduler.runScheduledTask()

        assertEquals("RUNNING", poll(gradingId)["status"].asString())
        assertEquals("RUNNING", poll(preparingId)["status"].asString())
        assertEquals(setOf(GradingJobStatus.RUNNING, GradingJobStatus.QUEUED), storedJobs().map { it.status }.toSet())
      } finally {
        releaseGrading.countDown()
      }
      grading.get(10, TimeUnit.SECONDS)
    }

    gradingScheduler.runScheduledTask()
    assertEquals("PASSED", poll(gradingId)["outcome"].asString())
    assertEquals("PASSED", poll(preparingId)["outcome"].asString())
    assertEquals(listOf("reference", "submitted", "reference", "submitted"), executor.calls)
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

  private fun enqueue(inputs: List<String> = listOf("1", "2", "3"), source: String = "user source"): String {
    val response = graphql(
      """mutation(${'$'}input: EnqueueCustomInputSubmissionInput!) {
        enqueueCustomInputSubmission(input: ${'$'}input) {
          __typename ... on EnqueueCustomInputSubmissionSuccess { customInputSubmissionId }
        }
      }""".trimIndent(),
      mapOf("input" to mapOf(
        "problemLanguageId" to globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configurationId),
        "sourceCode" to source,
        "cases" to inputs.map { mapOf("inputJson" to it) },
      )),
      session,
    )["enqueueCustomInputSubmission"]
    assertEquals("EnqueueCustomInputSubmissionSuccess", response["__typename"].asString(), response.toString())
    return response["customInputSubmissionId"].asString()
  }

  private fun poll(id: String, viewer: MockHttpSession? = session): JsonNode = graphql(
    """query(${'$'}id: ID!) { customInputSubmission(id: ${'$'}id) {
      status outcome totalCases passedCases runtimeMs startedAt finishedAt publicErrorMessage
      caseResults { outcome output publicErrorMessage testCase { position inputJson expectedOutputJson } }
    } }""".trimIndent(),
    mapOf("id" to id),
    viewer,
  )["customInputSubmission"]

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

  private fun configureSchedulers(executor: CodeExecutionService) {
    val properties = ExecutionProperties(runtimes = mapOf("kotlin" to DockerExecutionServiceTest.IMAGE))
    val workerReadiness = ScheduledWorkerReadiness(applicationAvailability, properties)
    val startup = ExecutionStartup(
      gradingJobService, customInputSubmissionService, executor, properties, workerReadiness,
    )
    gradingScheduler = GradingScheduler(gradingJobService, settingsService, CodeGrader(executor), startup)
    customScheduler = CustomInputSubmissionScheduler(customInputSubmissionService, settingsService, executor, startup)
  }

  private fun caseResults(result: JsonNode): List<JsonNode> {
    val results = result["caseResults"]
    return (0 until results.size()).map { results[it] }
  }

  private fun cases(customInputSubmissionId: Long): List<CustomTestCase> =
    entClient.customTestCases.indexes.customInputSubmissionId(customInputSubmissionId).query {
      orderBy(CustomTestCase.position.asc())
    }.all(fixtures).getOrThrow()

  private fun storedJobs() = entClient.gradingJobs.query().all(fixtures).getOrThrow()

  private fun storedCustomInputSubmission(customInputSubmissionId: Long): CustomInputSubmission =
    entClient.customInputSubmissions.findById(fixtures, customInputSubmissionId).getOrThrow()!!

  private fun outputs(inputs: List<JsonElement>, answers: List<JsonElement> = inputs) = CodeExecutionResult.Completed(
    ProgramStatus.SUCCEEDED,
    inputs.mapIndexed { index, input ->
      TestCaseExecutionResult(input, ProgramStatus.SUCCEEDED, answers[index], answers[index].toString())
    },
    runtimeMs = 42,
  )

  private class TestCodeExecutionService : CodeExecutionService {
    val calls = CopyOnWriteArrayList<String>()
    val sources = CopyOnWriteArrayList<String>()
    var cleanupFailure: RuntimeException? = null
    var reference: (List<JsonElement>) -> CodeExecutionResult = { inputs ->
      CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, inputs.map {
        TestCaseExecutionResult(it, ProgramStatus.SUCCEEDED, it, "private reference diagnostic")
      })
    }
    var submitted: (List<JsonElement>) -> CodeExecutionResult = { inputs ->
      CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, inputs.map {
        TestCaseExecutionResult(it, ProgramStatus.SUCCEEDED, it, it.toString())
      })
    }

    override fun isAvailable(runtime: String) = true

    override fun cleanUpInterruptedExecutions() {
      cleanupFailure?.let { throw it }
    }

    override fun executeCode(
      executionId: String,
      runtime: String,
      program: PreparedProgram,
      inputs: List<JsonElement>,
      timeLimitMs: Int,
      memoryLimitMb: Int,
    ): CodeExecutionResult {
      val referenceExecution = executionId.startsWith("custom-reference-")
      calls += if (referenceExecution) "reference" else "submitted"
      sources += program.sourceFiles.getValue("Solution.kt")
      return if (referenceExecution) reference(inputs) else submitted(inputs)
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  class ExecutionTestConfiguration {
    @Bean
    @Primary
    fun runtimeAvailability() = RuntimeAvailability { true }
  }

  companion object {
    private val runtimeWorkspace = Files.createTempDirectory("custom-execution-runtime-")

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

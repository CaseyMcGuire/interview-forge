package com.application.execution

import com.application.config.ExecutionProperties
import com.application.db.policies.CustomInputSubmissionPolicy
import com.application.ent.CustomInputSubmission
import com.application.ent.EntClient
import com.application.schema.CustomInputSubmissionOutcome
import com.application.schema.CustomInputSubmissionStatus
import com.application.schema.GradingCase
import com.application.schema.GradingJobStatus
import com.application.schema.ProblemDifficulty
import com.application.schema.ProblemSubmissionStatus
import com.application.schema.ProblemSubmissionVerdict
import com.application.security.CurrentUser
import com.application.services.CustomInputSubmissionService
import com.application.services.ExecutionAvailabilityService
import entkt.postgres.PostgresDriver
import entkt.runtime.privacy.ViewerContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

@Testcontainers
@SpringBootTest(properties = ["execution.worker-enabled=false"])
class CustomInputSubmissionCleanupIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated cleanup fixtures")
  private val cutoff = Instant.parse("2026-09-01T12:00:00Z")

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var customInputSubmissionService: CustomInputSubmissionService

  @Autowired
  lateinit var currentUser: CurrentUser

  @Autowired
  lateinit var properties: ExecutionProperties

  @Autowired
  lateinit var executionAvailabilityService: ExecutionAvailabilityService

  @Autowired
  lateinit var policy: CustomInputSubmissionPolicy

  @Autowired
  lateinit var dataSource: DataSource

  private var userId = 0L
  private var problemId = 0L
  private var configurationId = 0L

  @BeforeEach
  fun setUp() {
    entClient.customInputSubmissions.deleteMany(fixtures).getOrThrow()

    userId = entClient.users.create {
      email = "cleanup-${UUID.randomUUID()}@example.com"
      hashedPassword = "unused"
    }.saveAndLoad(fixtures).getOrThrow().id

    problemId = entClient.problems.create {
      slug = "cleanup-${UUID.randomUUID()}"
      title = "Cleanup fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
    }.saveAndLoad(fixtures).getOrThrow().id

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = this@CustomInputSubmissionCleanupIntegrationTest.problemId
      languageId = language.id
      starterCode = "starter"
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `only expired finished submissions and their cases are removed`() {
    val expired = createCustomInputSubmission(cutoff.minusSeconds(1))
    val atCutoff = createCustomInputSubmission(cutoff)
    val future = createCustomInputSubmission(cutoff.plusSeconds(1))
    val queued = createCustomInputSubmission(cutoff.minusSeconds(60), CustomInputSubmissionStatus.QUEUED)
    val preparing = createCustomInputSubmission(cutoff.minusSeconds(60), CustomInputSubmissionStatus.RUNNING)
    val awaitingGrading = createCustomInputSubmission(cutoff.minusSeconds(60), CustomInputSubmissionStatus.RUNNING)
    val grading = createCustomInputSubmission(cutoff.minusSeconds(60), CustomInputSubmissionStatus.RUNNING)
    val jobs = listOf(
      createGradingJob(awaitingGrading, GradingJobStatus.QUEUED),
      createGradingJob(grading, GradingJobStatus.RUNNING),
    )
    val problemSubmission = entClient.problemSubmissions.create {
      userId = this@CustomInputSubmissionCleanupIntegrationTest.userId
      problemId = this@CustomInputSubmissionCleanupIntegrationTest.problemId
      problemLanguageId = configurationId
      sourceCode = "accepted solution"
      totalCases = 1
      passedCases = 1
      status = ProblemSubmissionStatus.FINISHED
      verdict = ProblemSubmissionVerdict.ACCEPTED
    }.saveAndLoad(fixtures).getOrThrow()

    assertEquals(2, customInputSubmissionService.deleteExpiredCustomInputSubmissions(cutoff))

    for (submission in listOf(expired, atCutoff)) {
      assertNull(entClient.customInputSubmissions.findById(fixtures, submission.id).getOrThrow())
      assertTrue(cases(submission).isEmpty())
    }

    for (submission in listOf(future, queued, preparing, awaitingGrading, grading)) {
      assertNotNull(entClient.customInputSubmissions.findById(fixtures, submission.id).getOrThrow())
      assertEquals(1, cases(submission).size)
    }

    for (jobId in jobs) {
      assertNotNull(entClient.gradingJobs.findById(fixtures, jobId).getOrThrow())
    }
    assertNotNull(entClient.problemSubmissions.findById(fixtures, problemSubmission.id).getOrThrow())
  }

  @Test
  fun `each batch deletes the oldest expirations and repeated cleanup is safe`() {
    val newest = createCustomInputSubmission(cutoff.minusSeconds(1))
    val oldest = createCustomInputSubmission(cutoff.minusSeconds(3))
    val middle = createCustomInputSubmission(cutoff.minusSeconds(2))

    assertEquals(2, customInputSubmissionService.deleteExpiredCustomInputSubmissions(cutoff, batchSize = 2))
    assertNull(entClient.customInputSubmissions.findById(fixtures, oldest.id).getOrThrow())
    assertNull(entClient.customInputSubmissions.findById(fixtures, middle.id).getOrThrow())
    assertNotNull(entClient.customInputSubmissions.findById(fixtures, newest.id).getOrThrow())

    assertEquals(1, customInputSubmissionService.deleteExpiredCustomInputSubmissions(cutoff, batchSize = 2))
    assertEquals(0, customInputSubmissionService.deleteExpiredCustomInputSubmissions(cutoff, batchSize = 2))
    assertTrue(cases(newest).isEmpty())
  }

  @Test
  fun `a failed deletion rolls back the whole batch including its cases`() {
    val submissions = listOf(createCustomInputSubmission(cutoff), createCustomInputSubmission(cutoff))
    var deletionAttempted = false
    val failingClient = EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies {
        customInputSubmissions(policy)
      }
      hooks {
        customInputSubmissions {
          afterDelete {
            deletionAttempted = true
            error("Simulated failure after deletion")
          }
        }
      }
    }

    val failingService = CustomInputSubmissionService(failingClient, currentUser, properties, executionAvailabilityService)

    assertThrows(Exception::class.java) {
      failingService.deleteExpiredCustomInputSubmissions(cutoff)
    }

    assertTrue(deletionAttempted)
    for (submission in submissions) {
      assertNotNull(entClient.customInputSubmissions.findById(fixtures, submission.id).getOrThrow())
      assertEquals(1, cases(submission).size)
    }
    assertEquals(2, customInputSubmissionService.deleteExpiredCustomInputSubmissions(cutoff))
  }

  private fun createCustomInputSubmission(
    expiresAt: Instant,
    status: CustomInputSubmissionStatus = CustomInputSubmissionStatus.FINISHED,
  ): CustomInputSubmission = entClient.withTransaction { tx ->
    val submission = tx.customInputSubmissions.create {
      userId = this@CustomInputSubmissionCleanupIntegrationTest.userId
      problemLanguageId = configurationId
      sourceCode = "solution"
      totalCases = 1
      this.expiresAt = expiresAt
      this.status = status
    }.saveAndLoad(fixtures).getOrThrow()

    val testCase = tx.customTestCases.create {
      customInputSubmissionId = submission.id
      position = 0
      inputJson = JsonNull
      expectedOutputJson = JsonNull
    }.saveAndLoad(fixtures).getOrThrow()

    if (status == CustomInputSubmissionStatus.FINISHED) {
      tx.customInputSubmissions.update(submission.id) {
        outcome = CustomInputSubmissionOutcome.INTERNAL_ERROR
        finishedAt = cutoff.minusSeconds(1)
        caseResults = JsonArray(listOf(CustomTestCaseResult(testCase.id, TestCaseOutcome.NOT_RUN).toJson()))
      }.save(fixtures).getOrThrow()
    }

    submission
  }.getOrThrow()

  private fun createGradingJob(submission: CustomInputSubmission, status: GradingJobStatus): Long =
    entClient.gradingJobs.create {
      customInputSubmissionId = submission.id
      problemLanguageId = configurationId
      sourceCode = submission.sourceCode
      cases = listOf(GradingCase(cases(submission).single().id, JsonNull, JsonNull))
      this.status = status
    }.saveAndLoad(fixtures).getOrThrow().id

  private fun cases(submission: CustomInputSubmission) =
    entClient.customTestCases.indexes.customInputSubmissionId(submission.id).query()
      .all(fixtures).getOrThrow()

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

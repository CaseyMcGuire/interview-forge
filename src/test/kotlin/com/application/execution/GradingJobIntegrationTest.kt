package com.application.execution

import com.application.ent.CustomTestSuiteRun
import com.application.ent.EntClient
import com.application.ent.EntClientScope
import com.application.ent.GradingJob
import com.application.ent.Submission
import com.application.schema.GradingCase
import com.application.schema.GradingJobStatus
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.security.ExecutionAccess
import entkt.postgres.PostgresDriver
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import entkt.runtime.result.EntValidationException
import kotlinx.serialization.json.Json
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
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

@Testcontainers
@SpringBootTest
class GradingJobIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated grading-job fixtures")
  private val caseSnapshots = listOf(
    GradingCase(
      testCaseId = 1,
      inputJson = Json.parseToJsonElement("[2, 1]"),
      expectedOutputJson = JsonNull,
      visibility = TestCaseVisibility.HIDDEN,
    ),
    GradingCase(
      testCaseId = 2,
      inputJson = Json.parseToJsonElement("9007199254740993"),
      expectedOutputJson = Json.parseToJsonElement("1.0"),
      visibility = TestCaseVisibility.EXAMPLE,
    ),
  )

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var dataSource: DataSource

  private var ownerId = 0L
  private var problemId = 0L
  private var configurationId = 0L

  @BeforeEach
  fun setUp() {
    ownerId = createUser()

    problemId = entClient.problems.create {
      slug = "grading-job-${UUID.randomUUID()}"
      title = "Grading job fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = ownerId
    }.saveAndLoad(fixtures).getOrThrow().id

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = this@GradingJobIntegrationTest.problemId
      languageId = language.id
      starterCode = "starter"
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `jobs preserve ordered snapshots including JSON null and are unique per attempt`() {
    val submission = createSubmission()
    val run = createCustomRun()

    val officialJob = createJob(submissionId = submission.id)
    val customCases = caseSnapshots.map { it.copy(visibility = null) }
    val customJob = createJob(customRunId = run.id, cases = customCases)

    val stored = entClient.gradingJobs.indexes.submissionId(submission.id)
      .query {}
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()!!

    assertEquals(officialJob.id, stored.id)
    assertEquals(caseSnapshots, stored.cases)
    assertEquals(JsonNull, stored.cases[0].expectedOutputJson)
    assertEquals("1.0", stored.cases[1].expectedOutputJson.toString())
    assertEquals("submitted source", stored.sourceCode)
    assertEquals(GradingJobStatus.QUEUED, stored.status)
    assertNull(stored.startedAt)
    assertNull(stored.customTestSuiteRunId)
    assertNull(customJob.submissionId)
    assertEquals(run.id, customJob.customTestSuiteRunId)
    assertEquals(customCases, customJob.cases)

    assertDatabaseFailure("23505") { createJob(submissionId = submission.id) }
    assertDatabaseFailure("23505") { createJob(customRunId = run.id) }
  }

  @Test
  fun `database requires exactly one existing origin even without application validation`() {
    // Keep these checks on the actual migration rather than stopping at the policy's validator.
    val schemaClient = EntClient(PostgresDriver(dataSource, autoDdl = false))
    val submission = createSubmission()
    val run = createCustomRun()

    assertDatabaseFailure("23514") {
      createJob(client = schemaClient, viewer = fixtures)
    }
    assertDatabaseFailure("23514") {
      createJob(submission.id, run.id, client = schemaClient, viewer = fixtures)
    }
    assertDatabaseFailure("23503") {
      createJob(submissionId = Long.MAX_VALUE, client = schemaClient, viewer = fixtures)
    }
    assertDatabaseFailure("23503") {
      createJob(customRunId = Long.MAX_VALUE, client = schemaClient, viewer = fixtures)
    }
  }

  @Test
  fun `execution writes validate the origin source and nonempty suite`() {
    val submission = createSubmission()
    val run = createCustomRun()

    assertThrows(EntValidationException::class.java) { createJob() }
    assertThrows(EntValidationException::class.java) { createJob(submission.id, run.id) }

    assertThrows(EntValidationException::class.java) {
      createJob(submissionId = submission.id, cases = emptyList())
    }
    assertThrows(EntValidationException::class.java) {
      createJob(submissionId = submission.id, sourceCode = " ")
    }
    assertThrows(EntValidationException::class.java) {
      createJob(submissionId = submission.id, sourceCode = "x".repeat(50_001))
    }
  }

  @Test
  fun `jobs are private to execution including from owners and administrators`() {
    val job = createJob(submissionId = createSubmission().id)
    val unusedSubmission = createSubmission()
    val viewers = listOf(
      ViewerContext(Viewer.Anonymous),
      ViewerContext(Viewer.User(ownerId)),
      ViewerContext(Viewer.User(createUser())),
      ViewerContext(Viewer.User(createUser(UserRole.ADMIN))),
    )

    for (viewer in viewers) {
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.gradingJobs.findById(viewer, job.id).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        createJob(submissionId = unusedSubmission.id, viewer = viewer)
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.gradingJobs.update(job.id) {
          status = GradingJobStatus.RUNNING
        }.save(viewer).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.gradingJobs.deleteById(viewer, job.id).getOrThrow()
      }
    }

    entClient.gradingJobs.update(job.id) {
      status = GradingJobStatus.RUNNING
      startedAt = Instant.now()
    }.save(ExecutionAccess.context).getOrThrow()

    val running = entClient.gradingJobs.indexes.status(GradingJobStatus.RUNNING).query {
      where(GradingJob.id eq job.id)
    }.firstOrNull(ExecutionAccess.context).getOrThrow()!!

    assertNotNull(running.startedAt)
    assertEquals(caseSnapshots, running.cases)

    entClient.gradingJobs.deleteById(ExecutionAccess.context, job.id).getOrThrow()
    assertNull(entClient.gradingJobs.findById(ExecutionAccess.context, job.id).getOrThrow())
  }

  @Test
  fun `deleting an origin removes its job without leaving orphaned execution inputs`() {
    val submission = createSubmission()
    val officialJob = createJob(submissionId = submission.id)
    val run = createCustomRun()
    val customJob = createJob(customRunId = run.id)

    entClient.submissions.deleteById(fixtures, submission.id).getOrThrow()
    entClient.customTestSuiteRuns.deleteById(fixtures, run.id).getOrThrow()

    assertNull(entClient.gradingJobs.findById(ExecutionAccess.context, officialJob.id).getOrThrow())
    assertNull(entClient.gradingJobs.findById(ExecutionAccess.context, customJob.id).getOrThrow())
  }

  @Test
  fun `a rejected job rolls back its originating submission in the same transaction`() {
    var submissionId = 0L

    assertThrows(EntValidationException::class.java) {
      entClient.withTransaction { tx ->
        submissionId = createSubmission(tx).id
        createJob(submissionId = submissionId, cases = emptyList(), client = tx)
      }.getOrThrow()
    }

    assertNull(entClient.submissions.findById(fixtures, submissionId).getOrThrow())
    val job = entClient.gradingJobs.indexes.submissionId(submissionId).query {}
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()

    assertNull(job)
  }

  private fun createUser(role: UserRole = UserRole.USER): Long = entClient.users.create {
    email = "grading-job-${UUID.randomUUID()}@example.com"
    hashedPassword = "unused"
    this.role = role
  }.saveAndLoad(fixtures).getOrThrow().id

  private fun createSubmission(client: EntClientScope = entClient): Submission = client.submissions.create {
    userId = ownerId
    problemId = this@GradingJobIntegrationTest.problemId
    problemLanguageId = configurationId
    sourceCode = "submitted source"
    totalCases = caseSnapshots.size
  }.saveAndLoad(ExecutionAccess.context).getOrThrow()

  private fun createCustomRun(): CustomTestSuiteRun = entClient.customTestSuiteRuns.create {
    userId = ownerId
    problemLanguageId = configurationId
    expiresAt = Instant.now().plusSeconds(300)
    sourceCode = "submitted source"
    totalCases = caseSnapshots.size
  }.saveAndLoad(ExecutionAccess.context).getOrThrow()

  private fun createJob(
    submissionId: Long? = null,
    customRunId: Long? = null,
    sourceCode: String = "submitted source",
    cases: List<GradingCase> = caseSnapshots,
    client: EntClientScope = entClient,
    viewer: ViewerContext = ExecutionAccess.context,
  ): GradingJob = client.gradingJobs.create {
    this.submissionId = submissionId
    customTestSuiteRunId = customRunId
    problemLanguageId = configurationId
    this.sourceCode = sourceCode
    this.cases = cases
  }.saveAndLoad(viewer).getOrThrow()

  private fun assertDatabaseFailure(sqlState: String, action: () -> Unit) {
    val failure = assertThrows(Exception::class.java, action)
    val databaseFailure = generateSequence<Throwable>(failure) { it.cause }
      .filterIsInstance<SQLException>()
      .firstOrNull()

    assertNotNull(databaseFailure, "Expected a database constraint failure: $failure")
    assertEquals(sqlState, databaseFailure!!.sqlState)
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

package com.application.execution

import com.application.ent.CustomInputSubmission
import com.application.ent.EntClient
import com.application.ent.EntClientScope
import com.application.ent.GradingJob
import com.application.ent.ProblemSubmission
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
    val problemSubmission = createProblemSubmission()
    val run = createCustomInputSubmission()

    val officialJob = createJob(problemSubmissionId = problemSubmission.id)
    val customCases = caseSnapshots.map { it.copy(visibility = null) }
    val customJob = createJob(customInputSubmissionId = run.id, cases = customCases)

    val stored = entClient.gradingJobs.indexes.problemSubmissionId(problemSubmission.id)
      .query()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()!!

    assertEquals(officialJob.id, stored.id)
    assertEquals(caseSnapshots, stored.cases)
    assertEquals(JsonNull, stored.cases[0].expectedOutputJson)
    assertEquals("1.0", stored.cases[1].expectedOutputJson.toString())
    assertEquals("submitted source", stored.sourceCode)
    assertEquals(GradingJobStatus.QUEUED, stored.status)
    assertNull(stored.startedAt)
    assertNull(stored.customInputSubmissionId)
    assertNull(customJob.problemSubmissionId)
    assertEquals(run.id, customJob.customInputSubmissionId)
    assertEquals(customCases, customJob.cases)

    assertDatabaseFailure("23505") { createJob(problemSubmissionId = problemSubmission.id) }
    assertDatabaseFailure("23505") { createJob(customInputSubmissionId = run.id) }
  }

  @Test
  fun `database requires exactly one existing origin even without application validation`() {
    // Keep these checks on the actual migration rather than stopping at the policy's validator.
    val schemaClient = EntClient(PostgresDriver(dataSource, autoDdl = false))
    val problemSubmission = createProblemSubmission()
    val run = createCustomInputSubmission()

    assertDatabaseFailure("23514") {
      createJob(client = schemaClient, viewer = fixtures)
    }
    assertDatabaseFailure("23514") {
      createJob(problemSubmission.id, run.id, client = schemaClient, viewer = fixtures)
    }
    assertDatabaseFailure("23503") {
      createJob(problemSubmissionId = Long.MAX_VALUE, client = schemaClient, viewer = fixtures)
    }
    assertDatabaseFailure("23503") {
      createJob(customInputSubmissionId = Long.MAX_VALUE, client = schemaClient, viewer = fixtures)
    }
  }

  @Test
  fun `execution writes validate the origin source and nonempty suite`() {
    val problemSubmission = createProblemSubmission()
    val run = createCustomInputSubmission()

    assertThrows(EntValidationException::class.java) { createJob() }
    assertThrows(EntValidationException::class.java) { createJob(problemSubmission.id, run.id) }

    assertThrows(EntValidationException::class.java) {
      createJob(problemSubmissionId = problemSubmission.id, cases = emptyList())
    }
    assertThrows(EntValidationException::class.java) {
      createJob(problemSubmissionId = problemSubmission.id, sourceCode = " ")
    }
    assertThrows(EntValidationException::class.java) {
      createJob(problemSubmissionId = problemSubmission.id, sourceCode = "x".repeat(50_001))
    }
  }

  @Test
  fun `jobs are private to execution including from owners and administrators`() {
    val job = createJob(problemSubmissionId = createProblemSubmission().id)
    val unusedProblemSubmission = createProblemSubmission()
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
        createJob(problemSubmissionId = unusedProblemSubmission.id, viewer = viewer)
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
    val problemSubmission = createProblemSubmission()
    val officialJob = createJob(problemSubmissionId = problemSubmission.id)
    val run = createCustomInputSubmission()
    val customJob = createJob(customInputSubmissionId = run.id)

    entClient.problemSubmissions.deleteById(fixtures, problemSubmission.id).getOrThrow()
    entClient.customInputSubmissions.deleteById(fixtures, run.id).getOrThrow()

    assertNull(entClient.gradingJobs.findById(ExecutionAccess.context, officialJob.id).getOrThrow())
    assertNull(entClient.gradingJobs.findById(ExecutionAccess.context, customJob.id).getOrThrow())
  }

  @Test
  fun `a rejected job rolls back its originating submission in the same transaction`() {
    var problemSubmissionId = 0L

    assertThrows(EntValidationException::class.java) {
      entClient.withTransaction { tx ->
        problemSubmissionId = createProblemSubmission(tx).id
        createJob(problemSubmissionId = problemSubmissionId, cases = emptyList(), client = tx)
      }.getOrThrow()
    }

    assertNull(entClient.problemSubmissions.findById(fixtures, problemSubmissionId).getOrThrow())
    val job = entClient.gradingJobs.indexes.problemSubmissionId(problemSubmissionId).query()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()

    assertNull(job)
  }

  private fun createUser(role: UserRole = UserRole.USER): Long = entClient.users.create {
    email = "grading-job-${UUID.randomUUID()}@example.com"
    hashedPassword = "unused"
    this.role = role
  }.saveAndLoad(fixtures).getOrThrow().id

  private fun createProblemSubmission(client: EntClientScope = entClient): ProblemSubmission = client.problemSubmissions.create {
    userId = ownerId
    problemId = this@GradingJobIntegrationTest.problemId
    problemLanguageId = configurationId
    sourceCode = "submitted source"
    totalCases = caseSnapshots.size
  }.saveAndLoad(ExecutionAccess.context).getOrThrow()

  private fun createCustomInputSubmission(): CustomInputSubmission = entClient.customInputSubmissions.create {
    userId = ownerId
    problemLanguageId = configurationId
    expiresAt = Instant.now().plusSeconds(300)
    sourceCode = "submitted source"
    totalCases = caseSnapshots.size
  }.saveAndLoad(ExecutionAccess.context).getOrThrow()

  private fun createJob(
    problemSubmissionId: Long? = null,
    customInputSubmissionId: Long? = null,
    sourceCode: String = "submitted source",
    cases: List<GradingCase> = caseSnapshots,
    client: EntClientScope = entClient,
    viewer: ViewerContext = ExecutionAccess.context,
  ): GradingJob = client.gradingJobs.create {
    this.problemSubmissionId = problemSubmissionId
    this.customInputSubmissionId = customInputSubmissionId
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

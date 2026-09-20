package com.application.execution

import com.application.ent.CustomTestCase
import com.application.ent.CustomInputSubmission
import com.application.ent.EntClient
import com.application.schema.CustomInputSubmissionOutcome
import com.application.schema.CustomInputSubmissionStatus
import com.application.schema.ProblemDifficulty
import com.application.security.ExecutionAccess
import com.application.services.CustomInputSubmissionService
import entkt.runtime.privacy.ViewerContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
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

@Testcontainers
@SpringBootTest
class CustomInputSubmissionPersistenceIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated custom result fixtures")

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var service: CustomInputSubmissionService

  private var userId = 0L
  private var configurationId = 0L

  @BeforeEach
  fun setUp() {
    userId = entClient.users.create {
      email = "custom-result-${UUID.randomUUID()}@example.com"
      hashedPassword = "unused"
    }.saveAndLoad(fixtures).getOrThrow().id

    val problem = entClient.problems.create {
      slug = "custom-result-${UUID.randomUUID()}"
      title = "Custom result fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
    }.saveAndLoad(fixtures).getOrThrow()

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = problem.id
      languageId = language.id
      starterCode = "starter"
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `retains every result in input order and counts later passing cases`() {
    val run = createCustomInputSubmission()
    val cases = cases(run)
    val results = listOf(
      CustomTestCaseResult(cases[0].id, TestCaseOutcome.WRONG_ANSWER, "0"),
      CustomTestCaseResult(cases[1].id, TestCaseOutcome.INVALID_OUTPUT, "invalid JSON"),
      CustomTestCaseResult(cases[2].id, TestCaseOutcome.PASSED, "3"),
    )

    service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(
      outcome = CustomInputSubmissionOutcome.WRONG_ANSWER,
      caseResults = results.reversed(),
      runtimeMs = 42,
    ))

    val stored = storedCustomInputSubmission(run)
    assertEquals(CustomInputSubmissionStatus.FINISHED, stored.status)
    assertEquals(CustomInputSubmissionOutcome.WRONG_ANSWER, stored.outcome)
    assertEquals(1, stored.passedCases)
    assertEquals(42L, stored.runtimeMs)
    assertNotNull(stored.finishedAt)
    assertNull(stored.publicErrorMessage)
    assertEquals(results, retainedResults(run))
    assertEquals(listOf("1", "2", "3"), cases(run).map { it.expectedOutputJson.toString() })
  }

  @Test
  fun `passing results retain the complete summary and every output`() {
    val run = createCustomInputSubmission()
    val results = cases(run).map {
      CustomTestCaseResult(it.id, TestCaseOutcome.PASSED, it.expectedOutputJson.toString())
    }

    service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(
      outcome = CustomInputSubmissionOutcome.PASSED,
      caseResults = results,
      runtimeMs = 42,
    ))

    assertEquals(CustomInputSubmissionStatus.FINISHED, storedCustomInputSubmission(run).status)
    assertEquals(CustomInputSubmissionOutcome.PASSED, storedCustomInputSubmission(run).outcome)
    assertEquals(3, storedCustomInputSubmission(run).passedCases)
    assertNull(storedCustomInputSubmission(run).publicErrorMessage)
    assertEquals(results, retainedResults(run))
  }

  @Test
  fun `bounds stored output replaces NUL and fills in unreached cases`() {
    val run = createCustomInputSubmission()
    val cases = cases(run)
    val results = listOf(
      CustomTestCaseResult(cases[0].id, TestCaseOutcome.PASSED, "1"),
      CustomTestCaseResult(cases[1].id, TestCaseOutcome.TIME_LIMIT_EXCEEDED, "\u0000${"x".repeat(20_000)}"),
    )

    service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(
      outcome = CustomInputSubmissionOutcome.TIME_LIMIT_EXCEEDED,
      caseResults = results,
      runtimeMs = 50,
    ))

    val retained = retainedResults(run)
    assertEquals(1, storedCustomInputSubmission(run).passedCases)
    assertEquals(listOf(TestCaseOutcome.PASSED, TestCaseOutcome.TIME_LIMIT_EXCEEDED, TestCaseOutcome.NOT_RUN),
      retained.map { it.outcome })
    assertEquals(20_000, retained[1].output.length)
    assertTrue(retained[1].output.startsWith("\uFFFD"))
    assertEquals("", retained[2].output)
  }

  @Test
  fun `failures before user execution retain safe messages and NOT_RUN results`() {
    for (outcome in listOf(
      CustomInputSubmissionOutcome.COMPILE_ERROR,
      CustomInputSubmissionOutcome.REFERENCE_SOLUTION_FAILED,
      CustomInputSubmissionOutcome.INTERNAL_ERROR,
    )) {
      val run = createCustomInputSubmission()

      service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(outcome))

      assertEquals(outcome, storedCustomInputSubmission(run).outcome)
      assertEquals(0, storedCustomInputSubmission(run).passedCases)
      assertNull(storedCustomInputSubmission(run).runtimeMs)
      assertFalse(storedCustomInputSubmission(run).publicErrorMessage.isNullOrBlank())
      assertTrue(retainedResults(run).all { it.outcome == TestCaseOutcome.NOT_RUN && it.output.isEmpty() })
    }
  }

  @Test
  fun `finalization rejects foreign duplicate and incomplete passing results without changing the run`() {
    val run = createCustomInputSubmission()
    val otherRun = createCustomInputSubmission()
    val testCase = cases(run).first()
    val foreignCase = cases(otherRun).first()
    val invalidResults = listOf(
      CustomInputSubmissionResult(CustomInputSubmissionOutcome.PASSED),
      CustomInputSubmissionResult(CustomInputSubmissionOutcome.WRONG_ANSWER, listOf(
        CustomTestCaseResult(foreignCase.id, TestCaseOutcome.WRONG_ANSWER),
      )),
      CustomInputSubmissionResult(CustomInputSubmissionOutcome.WRONG_ANSWER, listOf(
        CustomTestCaseResult(testCase.id, TestCaseOutcome.PASSED),
        CustomTestCaseResult(testCase.id, TestCaseOutcome.WRONG_ANSWER),
      )),
      CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR, runtimeMs = -1),
    )

    for (result in invalidResults) {
      assertThrows(IllegalStateException::class.java) { service.finishCustomInputSubmission(run.id, result) }
      assertEquals(CustomInputSubmissionStatus.RUNNING, storedCustomInputSubmission(run).status)
      assertNull(storedCustomInputSubmission(run).caseResults)
    }

    service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR))
    assertThrows(IllegalStateException::class.java) {
      service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR))
    }
  }

  @Test
  fun `queued runs cannot receive final results`() {
    val run = createCustomInputSubmission(status = CustomInputSubmissionStatus.QUEUED)

    assertThrows(IllegalStateException::class.java) {
      service.finishCustomInputSubmission(run.id, CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR))
    }

    assertEquals(CustomInputSubmissionStatus.QUEUED, storedCustomInputSubmission(run).status)
    assertNull(storedCustomInputSubmission(run).caseResults)
  }

  private fun createCustomInputSubmission(
    status: CustomInputSubmissionStatus = CustomInputSubmissionStatus.RUNNING,
  ): CustomInputSubmission = entClient.withTransaction { tx ->
    val run = tx.customInputSubmissions.create {
      userId = this@CustomInputSubmissionPersistenceIntegrationTest.userId
      problemLanguageId = configurationId
      expiresAt = Instant.now().plusSeconds(300)
      sourceCode = "user solution"
      totalCases = 3
      this.status = status
      startedAt = if (status == CustomInputSubmissionStatus.RUNNING) Instant.now() else null
    }.saveAndLoad(fixtures).getOrThrow()

    for (position in 0..2) {
      tx.customTestCases.create {
        customInputSubmissionId = run.id
        this.position = position
        inputJson = JsonPrimitive(position + 1)
        expectedOutputJson = JsonPrimitive(position + 1)
      }.save(fixtures).getOrThrow()
    }

    run
  }.getOrThrow()

  private fun cases(run: CustomInputSubmission): List<CustomTestCase> =
    entClient.customTestCases.indexes.customInputSubmissionId(run.id).query {
      orderBy(CustomTestCase.position.asc())
    }.all(ExecutionAccess.context).getOrThrow()

  private fun storedCustomInputSubmission(run: CustomInputSubmission): CustomInputSubmission =
    entClient.customInputSubmissions.findById(ExecutionAccess.context, run.id).getOrThrow()!!

  private fun retainedResults(run: CustomInputSubmission): List<CustomTestCaseResult> =
    checkNotNull(storedCustomInputSubmission(run).caseResults).map { CustomTestCaseResult.fromJson(it.jsonObject) }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

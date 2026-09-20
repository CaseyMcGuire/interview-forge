package com.application.execution

import com.application.ent.CustomTestCase
import com.application.ent.CustomTestSuiteRun
import com.application.ent.EntClient
import com.application.schema.CustomTestSuiteRunOutcome
import com.application.schema.CustomTestSuiteRunStatus
import com.application.schema.ProblemDifficulty
import com.application.security.ExecutionAccess
import com.application.services.CustomTestSuiteRunService
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
class CustomTestSuiteRunPersistenceIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated custom result fixtures")

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var service: CustomTestSuiteRunService

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
    val run = createRun()
    val cases = cases(run)
    val results = listOf(
      CustomTestCaseResult(cases[0].id, TestCaseOutcome.WRONG_ANSWER, "0"),
      CustomTestCaseResult(cases[1].id, TestCaseOutcome.INVALID_OUTPUT, "invalid JSON"),
      CustomTestCaseResult(cases[2].id, TestCaseOutcome.PASSED, "3"),
    )

    service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(
      outcome = CustomTestSuiteRunOutcome.WRONG_ANSWER,
      caseResults = results.reversed(),
      runtimeMs = 42,
    ))

    val stored = storedRun(run)
    assertEquals(CustomTestSuiteRunStatus.FINISHED, stored.status)
    assertEquals(CustomTestSuiteRunOutcome.WRONG_ANSWER, stored.outcome)
    assertEquals(1, stored.passedCases)
    assertEquals(42L, stored.runtimeMs)
    assertNotNull(stored.finishedAt)
    assertNull(stored.publicErrorMessage)
    assertEquals(results, retainedResults(run))
    assertEquals(listOf("1", "2", "3"), cases(run).map { it.expectedOutputJson.toString() })
  }

  @Test
  fun `passing results retain the complete summary and every output`() {
    val run = createRun()
    val results = cases(run).map {
      CustomTestCaseResult(it.id, TestCaseOutcome.PASSED, it.expectedOutputJson.toString())
    }

    service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(
      outcome = CustomTestSuiteRunOutcome.PASSED,
      caseResults = results,
      runtimeMs = 42,
    ))

    assertEquals(CustomTestSuiteRunStatus.FINISHED, storedRun(run).status)
    assertEquals(CustomTestSuiteRunOutcome.PASSED, storedRun(run).outcome)
    assertEquals(3, storedRun(run).passedCases)
    assertNull(storedRun(run).publicErrorMessage)
    assertEquals(results, retainedResults(run))
  }

  @Test
  fun `bounds stored output replaces NUL and fills in unreached cases`() {
    val run = createRun()
    val cases = cases(run)
    val results = listOf(
      CustomTestCaseResult(cases[0].id, TestCaseOutcome.PASSED, "1"),
      CustomTestCaseResult(cases[1].id, TestCaseOutcome.TIME_LIMIT_EXCEEDED, "\u0000${"x".repeat(20_000)}"),
    )

    service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(
      outcome = CustomTestSuiteRunOutcome.TIME_LIMIT_EXCEEDED,
      caseResults = results,
      runtimeMs = 50,
    ))

    val retained = retainedResults(run)
    assertEquals(1, storedRun(run).passedCases)
    assertEquals(listOf(TestCaseOutcome.PASSED, TestCaseOutcome.TIME_LIMIT_EXCEEDED, TestCaseOutcome.NOT_RUN),
      retained.map { it.outcome })
    assertEquals(20_000, retained[1].output.length)
    assertTrue(retained[1].output.startsWith("\uFFFD"))
    assertEquals("", retained[2].output)
  }

  @Test
  fun `failures before user execution retain safe messages and NOT_RUN results`() {
    for (outcome in listOf(
      CustomTestSuiteRunOutcome.COMPILE_ERROR,
      CustomTestSuiteRunOutcome.REFERENCE_SOLUTION_FAILED,
      CustomTestSuiteRunOutcome.INTERNAL_ERROR,
    )) {
      val run = createRun()

      service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(outcome))

      assertEquals(outcome, storedRun(run).outcome)
      assertEquals(0, storedRun(run).passedCases)
      assertNull(storedRun(run).runtimeMs)
      assertFalse(storedRun(run).publicErrorMessage.isNullOrBlank())
      assertTrue(retainedResults(run).all { it.outcome == TestCaseOutcome.NOT_RUN && it.output.isEmpty() })
    }
  }

  @Test
  fun `finalization rejects foreign duplicate and incomplete passing results without changing the run`() {
    val run = createRun()
    val otherRun = createRun()
    val testCase = cases(run).first()
    val foreignCase = cases(otherRun).first()
    val invalidResults = listOf(
      CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.PASSED),
      CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.WRONG_ANSWER, listOf(
        CustomTestCaseResult(foreignCase.id, TestCaseOutcome.WRONG_ANSWER),
      )),
      CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.WRONG_ANSWER, listOf(
        CustomTestCaseResult(testCase.id, TestCaseOutcome.PASSED),
        CustomTestCaseResult(testCase.id, TestCaseOutcome.WRONG_ANSWER),
      )),
      CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR, runtimeMs = -1),
    )

    for (result in invalidResults) {
      assertThrows(IllegalStateException::class.java) { service.finishCustomTestSuiteRun(run.id, result) }
      assertEquals(CustomTestSuiteRunStatus.RUNNING, storedRun(run).status)
      assertNull(storedRun(run).caseResults)
    }

    service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR))
    assertThrows(IllegalStateException::class.java) {
      service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR))
    }
  }

  @Test
  fun `queued runs cannot receive final results`() {
    val run = createRun(status = CustomTestSuiteRunStatus.QUEUED)

    assertThrows(IllegalStateException::class.java) {
      service.finishCustomTestSuiteRun(run.id, CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR))
    }

    assertEquals(CustomTestSuiteRunStatus.QUEUED, storedRun(run).status)
    assertNull(storedRun(run).caseResults)
  }

  private fun createRun(
    status: CustomTestSuiteRunStatus = CustomTestSuiteRunStatus.RUNNING,
  ): CustomTestSuiteRun = entClient.withTransaction { tx ->
    val run = tx.customTestSuiteRuns.create {
      userId = this@CustomTestSuiteRunPersistenceIntegrationTest.userId
      problemLanguageId = configurationId
      expiresAt = Instant.now().plusSeconds(300)
      sourceCode = "user solution"
      totalCases = 3
      this.status = status
      startedAt = if (status == CustomTestSuiteRunStatus.RUNNING) Instant.now() else null
    }.saveAndLoad(fixtures).getOrThrow()

    for (position in 0..2) {
      tx.customTestCases.create {
        customTestSuiteRunId = run.id
        this.position = position
        inputJson = JsonPrimitive(position + 1)
        expectedOutputJson = JsonPrimitive(position + 1)
      }.save(fixtures).getOrThrow()
    }

    run
  }.getOrThrow()

  private fun cases(run: CustomTestSuiteRun): List<CustomTestCase> =
    entClient.customTestCases.indexes.customTestSuiteRunId(run.id).query {
      orderBy(CustomTestCase.position.asc())
    }.all(ExecutionAccess.context).getOrThrow()

  private fun storedRun(run: CustomTestSuiteRun): CustomTestSuiteRun =
    entClient.customTestSuiteRuns.findById(ExecutionAccess.context, run.id).getOrThrow()!!

  private fun retainedResults(run: CustomTestSuiteRun): List<CustomTestCaseResult> =
    checkNotNull(storedRun(run).caseResults).map { CustomTestCaseResult.fromJson(it.jsonObject) }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

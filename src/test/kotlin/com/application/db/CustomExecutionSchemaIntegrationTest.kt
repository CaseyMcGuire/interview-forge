package com.application.db

import com.application.ent.CustomTestCase
import com.application.ent.CustomTestSuite
import com.application.ent.CustomTestSuiteRun
import com.application.ent.EntClient
import com.application.schema.CustomTestSuiteRunOutcome
import com.application.schema.CustomTestSuiteRunStatus
import com.application.schema.ProblemDifficulty
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntConstraintViolationException
import entkt.runtime.result.EntPrivacyDeniedException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
class CustomExecutionSchemaIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect isolated custom execution storage fixtures before service policies are implemented",
  )

  @Autowired
  lateinit var entClient: EntClient

  private var userId = 0L
  private var configurationId = 0L

  @BeforeEach
  fun createProblemLanguage() {
    userId = entClient.users.create {
      email = "custom-execution-${UUID.randomUUID()}@example.com"
      hashedPassword = "unused"
    }.saveAndLoad(fixtures).getOrThrow().id

    val problem = entClient.problems.create {
      slug = "custom-execution-${UUID.randomUUID()}"
      title = "Custom execution fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
    }.saveAndLoad(fixtures).getOrThrow()

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = problem.id
      languageId = language.id
      starterCode = "fun solve(value: Int) = value"
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `ordered cases distinguish missing expectations from prepared JSON null`() {
    val suite = createSuite()
    val second = createCase(suite, 1)
    val first = createCase(suite, 0)

    entClient.customTestCases.update(first.id) {
      expectedOutputJson = JsonNull
    }.save(fixtures).getOrThrow()

    val stored = entClient.customTestSuites.query {
      where(CustomTestSuite.id eq suite.id)
      loadCases { orderBy(CustomTestCase.position.asc()) }
    }.firstOrNull(fixtures).getOrThrow()!!
    val cases = stored.edges.cases.requireLoaded()

    assertEquals(userId, stored.userId)
    assertEquals(configurationId, stored.problemLanguageId)
    assertEquals(listOf(first.id, second.id), cases.map { it.id })
    assertEquals(JsonNull, cases[0].expectedOutputJson)
    assertNull(cases[1].expectedOutputJson)
  }

  @Test
  fun `one run retains all case results as a JSON array`() {
    val suite = createSuite()
    val first = createCase(suite, 0)
    val second = createCase(suite, 1)
    val run = createRun(suite, 2)

    assertEquals(CustomTestSuiteRunStatus.QUEUED, run.status)
    assertEquals(0, run.passedCases)
    assertNull(run.outcome)
    assertNull(run.caseResults)

    val results = buildJsonArray {
      addJsonObject {
        put("testCaseId", first.id)
        put("outcome", "PASSED")
        put("output", "0")
      }
      addJsonObject {
        put("testCaseId", second.id)
        put("outcome", "WRONG_ANSWER")
        put("output", "2")
      }
    }

    entClient.customTestSuiteRuns.update(run.id) {
      status = CustomTestSuiteRunStatus.FINISHED
      outcome = CustomTestSuiteRunOutcome.WRONG_ANSWER
      passedCases = 1
      caseResults = results
      startedAt = Instant.now().minusSeconds(1)
      finishedAt = Instant.now()
    }.save(fixtures).getOrThrow()

    val stored = entClient.customTestSuiteRuns.indexes.customTestSuiteId(suite.id)
      .find(fixtures)
      .getOrThrow()!!

    assertEquals(run.id, stored.id)
    assertEquals(results, stored.caseResults)
    assertEquals(CustomTestSuiteRunOutcome.WRONG_ANSWER, stored.outcome)
    assertEquals(1, stored.passedCases)
    assertNotNull(stored.finishedAt)
  }

  @Test
  fun `case positions are unique within a suite`() {
    val suite = createSuite()
    val first = createCase(suite, 0)
    val otherSuite = createSuite()
    createCase(otherSuite, 0)

    val duplicate = assertThrows(EntConstraintViolationException::class.java) {
      createCase(suite, 0)
    }
    assertEquals("uq_custom_test_cases_suite_position", duplicate.constraint)

    val retained = entClient.customTestCases.indexes.customTestSuiteId(suite.id)
      .position(0)
      .find(fixtures)
      .getOrThrow()
    assertEquals(first.id, retained!!.id)
  }

  @Test
  fun `each run requires its own suite`() {
    val suite = createSuite()
    createCase(suite, 0)
    val run = createRun(suite, 1)

    val duplicate = assertThrows(EntConstraintViolationException::class.java) {
      createRun(suite, 1)
    }
    assertEquals("idx_custom_test_suite_runs_custom_test_suite_id_unique", duplicate.constraint)

    val stored = entClient.customTestSuites.query {
      where(CustomTestSuite.id eq suite.id)
      loadRun()
    }.firstOrNull(fixtures).getOrThrow()!!
    assertEquals(run.id, stored.edges.run.requireLoaded()!!.id)

    val nextSuite = createSuite()
    createCase(nextSuite, 0)
    val nextRun = createRun(nextSuite, 1)
    assertEquals(nextSuite.id, nextRun.customTestSuiteId)
  }

  @Test
  fun `deleting a suite cascades to its cases and run without affecting another suite`() {
    val suite = createSuite()
    val testCase = createCase(suite, 0)
    val run = createRun(suite, 1)
    val otherSuite = createSuite()
    val otherCase = createCase(otherSuite, 0)
    val otherRun = createRun(otherSuite, 1)

    entClient.customTestSuites.deleteById(fixtures, suite.id).getOrThrow()

    assertNull(entClient.customTestCases.findById(fixtures, testCase.id).getOrThrow())
    assertNull(entClient.customTestSuiteRuns.findById(fixtures, run.id).getOrThrow())
    assertNotNull(entClient.customTestSuites.findById(fixtures, otherSuite.id).getOrThrow())
    assertNotNull(entClient.customTestCases.findById(fixtures, otherCase.id).getOrThrow())
    assertNotNull(entClient.customTestSuiteRuns.findById(fixtures, otherRun.id).getOrThrow())
  }

  @Test
  fun `a retained suite prevents deleting its language configuration`() {
    val suite = createSuite()

    val referenced = assertThrows(EntConstraintViolationException::class.java) {
      entClient.problemLanguages.deleteById(fixtures, configurationId).getOrThrow()
    }
    assertEquals("fk_custom_test_suites_problem_language_id", referenced.constraint)

    entClient.customTestSuites.deleteById(fixtures, suite.id).getOrThrow()
    assertTrue(entClient.problemLanguages.deleteById(fixtures, configurationId).getOrThrow())
  }

  @Test
  fun `reference code is optional private and editable on the existing judge configuration`() {
    val judge = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      testDriverCode = "private driver"
      timeLimitMs = 1000
      memoryLimitMb = 256
    }.saveAndLoad(fixtures).getOrThrow()
    assertNull(judge.referenceSolutionCode)

    val referenceCode = "fun solve(value: Int) = value"
    entClient.judgeConfigurations.update(judge.id) {
      referenceSolutionCode = referenceCode
    }.save(fixtures).getOrThrow()

    val stored = entClient.judgeConfigurations.findById(fixtures, judge.id).getOrThrow()!!
    assertEquals(referenceCode, stored.referenceSolutionCode)
    assertFalse(stored.toString().contains(referenceCode))
    assertThrows(EntPrivacyDeniedException::class.java) {
      entClient.judgeConfigurations.findById(ViewerContext(Viewer.Anonymous), judge.id).getOrThrow()
    }

    entClient.judgeConfigurations.update(judge.id) {
      referenceSolutionCode = null
    }.save(fixtures).getOrThrow()
    assertNull(entClient.judgeConfigurations.findById(fixtures, judge.id).getOrThrow()!!.referenceSolutionCode)
  }

  @Test
  fun `custom execution data is not publicly readable`() {
    val suite = createSuite()
    val testCase = createCase(suite, 0)
    val run = createRun(suite, 1)
    val anonymous = ViewerContext(Viewer.Anonymous)

    assertThrows(EntPrivacyDeniedException::class.java) {
      entClient.customTestSuites.findById(anonymous, suite.id).getOrThrow()
    }
    assertThrows(EntPrivacyDeniedException::class.java) {
      entClient.customTestCases.findById(anonymous, testCase.id).getOrThrow()
    }
    assertThrows(EntPrivacyDeniedException::class.java) {
      entClient.customTestSuiteRuns.findById(anonymous, run.id).getOrThrow()
    }
  }

  private fun createSuite(): CustomTestSuite = entClient.customTestSuites.create {
    userId = this@CustomExecutionSchemaIntegrationTest.userId
    problemLanguageId = configurationId
    expiresAt = Instant.now().plusSeconds(24 * 60 * 60)
  }.saveAndLoad(fixtures).getOrThrow()

  private fun createCase(suite: CustomTestSuite, position: Int): CustomTestCase = entClient.customTestCases.create {
    customTestSuiteId = suite.id
    this.position = position
    inputJson = JsonPrimitive(position)
  }.saveAndLoad(fixtures).getOrThrow()

  private fun createRun(
    suite: CustomTestSuite,
    caseCount: Int,
  ): CustomTestSuiteRun = entClient.customTestSuiteRuns.create {
    customTestSuiteId = suite.id
    sourceCode = "fun solve(value: Int) = value"
    totalCases = caseCount
  }.saveAndLoad(fixtures).getOrThrow()

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

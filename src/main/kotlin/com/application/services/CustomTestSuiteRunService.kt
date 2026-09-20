package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.CustomTestCase
import com.application.ent.CustomTestSuiteRun
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.security.CurrentUser
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntValidationException
import entkt.runtime.result.visibleOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

@Service
class CustomTestSuiteRunService(
  private val entClient: EntClient,
  private val currentUser: CurrentUser,
  private val properties: ExecutionProperties,
  private val executionAvailabilityService: ExecutionAvailabilityService,
) {
  private val publicContext = ViewerContext(Viewer.Anonymous)
  private val inputMapper = JsonMapper.builder(
    JsonFactory.builder()
      .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(20_000).build())
      .build(),
  )
    .enable(
      DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
      DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
      DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
      DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
    )
    .build()

  fun enqueueCustomTestSuiteRun(
    problemLanguageId: Long?,
    sourceCode: String,
    caseInputs: List<String>,
  ): EnqueueCustomTestSuiteRunOutcome {
    val user = currentUser.get() ?: return EnqueueCustomTestSuiteRunOutcome.AuthenticationRequired
    val configurationId = problemLanguageId ?: return EnqueueCustomTestSuiteRunOutcome.NotFound
    validateCaseCount(caseInputs.size)

    // Concurrent admissions may fail with a serialization error; revisit retries if contention becomes an issue.
    return entClient.withTransaction(IsolationLevel.Serializable) { tx ->
      enqueueCustomTestSuiteRun(tx, user.id, configurationId, sourceCode, caseInputs)
    }.getOrThrow()
  }

  private fun enqueueCustomTestSuiteRun(
    tx: EntTransactionClient,
    userId: Long,
    configurationId: Long,
    sourceCode: String,
    caseInputs: List<String>,
  ): EnqueueCustomTestSuiteRunOutcome {
    val configuration = tx.problemLanguages.findById(publicContext, configurationId)
      .visibleOrNull()
      .getOrThrow()
      ?: return EnqueueCustomTestSuiteRunOutcome.NotFound

    val problem = tx.problems.findById(publicContext, configuration.problemId)
      .visibleOrNull()
      .getOrThrow()
      ?: return EnqueueCustomTestSuiteRunOutcome.NotFound

    val language = tx.languages.findById(publicContext, configuration.languageId)
      .visibleOrNull()
      .getOrThrow()
      ?: return EnqueueCustomTestSuiteRunOutcome.NotFound

    val judge = executionAvailabilityService.findExecutableJudgeConfiguration(tx, problem, configuration.id, language.key)
    if (judge?.referenceSolutionCode.isNullOrBlank()) {
      return EnqueueCustomTestSuiteRunOutcome.Unavailable
    }

    if (!executionAvailabilityService.hasExecutionCapacity(tx, userId)) {
      return EnqueueCustomTestSuiteRunOutcome.Busy
    }

    val runId = createQueuedRun(tx, userId, configurationId, sourceCode, caseInputs.size)
    createCustomTestCases(tx, runId, caseInputs)

    return EnqueueCustomTestSuiteRunOutcome.Success(runId)
  }

  private fun validateCaseCount(count: Int) {
    if (count !in 1..properties.maxCustomTestCases) {
      throw EnqueueCustomTestSuiteRunInputException(
        "cases",
        "Provide between 1 and ${properties.maxCustomTestCases} test cases",
      )
    }
  }

  private fun createCustomTestCases(tx: EntTransactionClient, runId: Long, inputs: List<String>) {
    inputs.forEachIndexed { position, input ->
      createCustomTestCase(tx, runId, position, parseCaseInput(input, position))
    }
  }

  private fun createCustomTestCase(
    tx: EntTransactionClient,
    runId: Long,
    position: Int,
    input: JsonElement,
  ) {
    try {
      tx.customTestCases.create {
        customTestSuiteRunId = runId
        this.position = position
        inputJson = input
      }.save(ExecutionAccess.context).getOrThrow()
    } catch (exception: EntValidationException) {
      val violation = exception.violations.first()
      throw EnqueueCustomTestSuiteRunInputException("cases[$position].${violation.field}", violation.message)
    }
  }

  private fun parseCaseInput(input: String, position: Int): JsonElement {
    val parsed = try {
      inputMapper.readTree(input)
    } catch (_: JacksonException) {
      throw EnqueueCustomTestSuiteRunInputException("cases[$position].inputJson", "Provide valid JSON")
    }

    if (parsed == null || parsed.isMissingNode) {
      throw EnqueueCustomTestSuiteRunInputException("cases[$position].inputJson", "Provide valid JSON")
    }

    // Keep the original numeric representation; normalizing 1.0 to 1 would change strict JSON comparisons.
    return Json.parseToJsonElement(input)
  }

  private fun createQueuedRun(
    tx: EntTransactionClient,
    userId: Long,
    configurationId: Long,
    sourceCode: String,
    caseCount: Int,
  ): Long = tx.customTestSuiteRuns.create {
    this.userId = userId
    problemLanguageId = configurationId
    expiresAt = Instant.now().plus(properties.customTestSuiteLifetime)
    this.sourceCode = sourceCode
    totalCases = caseCount
  }.saveAndLoad(ExecutionAccess.context).getOrThrow().id

  /** Loads the retained inputs through owner policies, independently of current problem availability. */
  fun findRunForCurrentUser(id: Long): CustomTestSuiteRun? {
    val user = currentUser.get() ?: return null
    val viewer = ViewerContext(Viewer.User(user.id))

    return entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      tx.customTestSuiteRuns.query {
        where(CustomTestSuiteRun.id eq id)
        loadCases { orderBy(CustomTestCase.position.asc()) }
      }.firstOrNull(viewer).visibleOrNull().getOrThrow()
    }.getOrThrow()
  }
}

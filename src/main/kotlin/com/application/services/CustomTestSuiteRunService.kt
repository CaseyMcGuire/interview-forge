package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.CustomTestCase
import com.application.ent.CustomTestSuiteRun
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.execution.CustomTestCaseResult
import com.application.execution.CustomTestSuiteRunResult
import com.application.execution.TestCaseOutcome
import com.application.execution.customTestSuiteRunErrorMessage
import com.application.schema.GradingCase
import com.application.schema.CustomTestSuiteRunOutcome
import com.application.schema.CustomTestSuiteRunStatus
import com.application.security.CurrentUser
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntValidationException
import entkt.runtime.result.visibleOrNull
import entkt.runtime.query.requireLoaded
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

  fun claimNextQueuedCustomRun(): CustomTestSuiteRun? = entClient.withTransaction { tx ->
    val run = tx.customTestSuiteRuns.indexes.status(CustomTestSuiteRunStatus.QUEUED).query {
      orderBy(CustomTestSuiteRun.createdAt.asc())
      orderBy(CustomTestSuiteRun.id.asc())
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return@withTransaction null

    tx.customTestSuiteRuns.update(run.id) {
      status = CustomTestSuiteRunStatus.RUNNING
      startedAt = Instant.now()
    }.saveAndLoad(ExecutionAccess.context).getOrThrow()
  }.getOrThrow()

  fun loadCustomInputs(runId: Long): List<JsonElement> =
    entClient.customTestCases.indexes.customTestSuiteRunId(runId).query {
      orderBy(CustomTestCase.position.asc())
    }.all(ExecutionAccess.context).getOrThrow().map { it.inputJson }

  /** Saves all answers and the ready job together; a failed write leaves the run unprepared. */
  fun saveExpectedOutputsAndEnqueueGradingJob(runId: Long, expectedOutputs: List<JsonElement>) {
    entClient.withTransaction { tx ->
      val run = lockRunningCustomRun(tx, runId)
      val cases = loadRunCases(tx, runId)

      check(cases.isNotEmpty() && cases.size == expectedOutputs.size) { "Expected outputs must match every custom input" }
      check(cases.all { it.expectedOutputJson == null }) { "Custom inputs are already prepared" }

      cases.forEachIndexed { index, testCase ->
        tx.customTestCases.update(testCase.id) {
          expectedOutputJson = expectedOutputs[index]
        }.save(ExecutionAccess.context).getOrThrow()
      }

      tx.gradingJobs.create {
        customTestSuiteRunId = run.id
        problemLanguageId = run.problemLanguageId
        sourceCode = run.sourceCode
        this.cases = cases.mapIndexed { index, testCase ->
          GradingCase(testCase.id, testCase.inputJson, expectedOutputs[index])
        }
      }.save(ExecutionAccess.context).getOrThrow()
    }.getOrThrow()
  }

  /** Startup recovery only; a custom run with a job has completed reference preparation. */
  fun finishInterruptedCustomPreparations() {
    val runs = entClient.customTestSuiteRuns.indexes.status(CustomTestSuiteRunStatus.RUNNING).query {
      loadGradingJob()
    }.all(ExecutionAccess.context).getOrThrow()

    for (run in runs) {
      if (run.edges.gradingJob.requireLoaded() != null) {
        continue
      }

      finishInterruptedCustomPreparation(run.id)
    }
  }

  /** Recovers one preparation owned by this scheduler after an uncertain write. */
  fun finishInterruptedCustomPreparation(runId: Long) {
    entClient.withTransaction { tx ->
      val run = tx.customTestSuiteRuns.query { where(CustomTestSuiteRun.id eq runId) }
        .forUpdate()
        .firstOrNull(ExecutionAccess.context)
        .getOrThrow() ?: return@withTransaction

      if (run.status != CustomTestSuiteRunStatus.RUNNING) {
        return@withTransaction
      }

      val job = tx.gradingJobs.indexes.customTestSuiteRunId(runId).query()
        .firstOrNull(ExecutionAccess.context).getOrThrow()
      if (job != null) {
        return@withTransaction
      }

      finishCustomTestSuiteRun(tx, runId, CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR))
    }.getOrThrow()
  }

  /** Saves one summary and one result array; missing case results become NOT_RUN. */
  fun finishCustomTestSuiteRun(runId: Long, result: CustomTestSuiteRunResult) {
    entClient.withTransaction { tx ->
      finishCustomTestSuiteRun(tx, runId, result)
    }.getOrThrow()
  }

  /** Joins job completion so retaining all custom results and deleting the job are atomic. */
  internal fun finishCustomTestSuiteRun(tx: EntTransactionClient, runId: Long, result: CustomTestSuiteRunResult) {
    val run = lockRunningCustomRun(tx, runId)
    val cases = loadRunCases(tx, run.id)
    val caseResults = completeCaseResults(cases, result)

    tx.customTestSuiteRuns.update(runId) {
      status = CustomTestSuiteRunStatus.FINISHED
      outcome = result.outcome
      passedCases = caseResults.count { it.outcome == TestCaseOutcome.PASSED }
      this.caseResults = JsonArray(caseResults.map { it.toJson() })
      runtimeMs = result.runtimeMs
      publicErrorMessage = customTestSuiteRunErrorMessage(result.outcome)
      finishedAt = Instant.now()
    }.save(ExecutionAccess.context).getOrThrow()
  }

  private fun lockRunningCustomRun(tx: EntTransactionClient, runId: Long): CustomTestSuiteRun {
    val run = tx.customTestSuiteRuns.query { where(CustomTestSuiteRun.id eq runId) }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: error("Custom test suite run is missing")

    check(run.status == CustomTestSuiteRunStatus.RUNNING) { "Custom test suite run is not running" }
    return run
  }

  private fun loadRunCases(tx: EntTransactionClient, runId: Long): List<CustomTestCase> =
    tx.customTestCases.indexes.customTestSuiteRunId(runId).query {
      orderBy(CustomTestCase.position.asc())
    }.all(ExecutionAccess.context).getOrThrow()

  private fun completeCaseResults(
    cases: List<CustomTestCase>,
    result: CustomTestSuiteRunResult,
  ): List<CustomTestCaseResult> {
    val resultsByCaseId = result.caseResults.associateBy { it.testCaseId }
    val caseIds = cases.map { it.id }.toSet()
    check(resultsByCaseId.size == result.caseResults.size) { "Duplicate custom case results" }
    check(caseIds.containsAll(resultsByCaseId.keys)) { "Results belong to another custom run" }
    check(result.runtimeMs == null || result.runtimeMs >= 0) { "Invalid custom run duration" }

    val completeResults = cases.map { testCase ->
      val caseResult = resultsByCaseId[testCase.id]
        ?: CustomTestCaseResult(testCase.id, TestCaseOutcome.NOT_RUN)

      // PostgreSQL JSONB rejects NUL characters. Retain only bounded user-program output.
      caseResult.copy(output = caseResult.output.replace('\u0000', '\uFFFD').take(20_000))
    }

    if (result.outcome == CustomTestSuiteRunOutcome.PASSED) {
      check(completeResults.isNotEmpty() && completeResults.all { it.outcome == TestCaseOutcome.PASSED }) {
        "A passing custom run must pass every case"
      }
    }

    return completeResults
  }
}

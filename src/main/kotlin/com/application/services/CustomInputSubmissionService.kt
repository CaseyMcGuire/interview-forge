package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.CustomTestCase
import com.application.ent.CustomInputSubmission
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.execution.CustomTestCaseResult
import com.application.execution.CustomInputSubmissionResult
import com.application.execution.TestCaseOutcome
import com.application.execution.customInputSubmissionErrorMessage
import com.application.schema.GradingCase
import com.application.schema.CustomInputSubmissionOutcome
import com.application.schema.CustomInputSubmissionStatus
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
class CustomInputSubmissionService(
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

  fun enqueueCustomInputSubmission(
    problemLanguageId: Long?,
    sourceCode: String,
    caseInputs: List<String>,
  ): EnqueueCustomInputSubmissionOutcome {
    val user = currentUser.get() ?: return EnqueueCustomInputSubmissionOutcome.AuthenticationRequired
    val configurationId = problemLanguageId ?: return EnqueueCustomInputSubmissionOutcome.NotFound
    validateCaseCount(caseInputs.size)

    // Concurrent admissions may fail with a serialization error; revisit retries if contention becomes an issue.
    return entClient.withTransaction(IsolationLevel.Serializable) { tx ->
      enqueueCustomInputSubmission(tx, user.id, configurationId, sourceCode, caseInputs)
    }.getOrThrow()
  }

  private fun enqueueCustomInputSubmission(
    tx: EntTransactionClient,
    userId: Long,
    configurationId: Long,
    sourceCode: String,
    caseInputs: List<String>,
  ): EnqueueCustomInputSubmissionOutcome {
    val configuration = tx.problemLanguages.findById(publicContext, configurationId)
      .visibleOrNull()
      .getOrThrow()
      ?: return EnqueueCustomInputSubmissionOutcome.NotFound

    val problem = tx.problems.findById(publicContext, configuration.problemId)
      .visibleOrNull()
      .getOrThrow()
      ?: return EnqueueCustomInputSubmissionOutcome.NotFound

    val language = tx.languages.findById(publicContext, configuration.languageId)
      .visibleOrNull()
      .getOrThrow()
      ?: return EnqueueCustomInputSubmissionOutcome.NotFound

    val judge = executionAvailabilityService.findExecutableJudgeConfiguration(tx, problem, configuration.id, language.key)
    if (judge?.referenceSolutionCode.isNullOrBlank()) {
      return EnqueueCustomInputSubmissionOutcome.Unavailable
    }

    if (!executionAvailabilityService.hasExecutionCapacity(tx, userId)) {
      return EnqueueCustomInputSubmissionOutcome.Busy
    }

    val customInputSubmissionId = createQueuedCustomInputSubmission(tx, userId, configurationId, sourceCode, caseInputs.size)
    createCustomTestCases(tx, customInputSubmissionId, caseInputs)

    return EnqueueCustomInputSubmissionOutcome.Success(customInputSubmissionId)
  }

  private fun validateCaseCount(count: Int) {
    if (count !in 1..properties.maxCustomTestCases) {
      throw EnqueueCustomInputSubmissionInputException(
        "cases",
        "Provide between 1 and ${properties.maxCustomTestCases} test cases",
      )
    }
  }

  private fun createCustomTestCases(tx: EntTransactionClient, customInputSubmissionId: Long, inputs: List<String>) {
    inputs.forEachIndexed { position, input ->
      createCustomTestCase(tx, customInputSubmissionId, position, parseCaseInput(input, position))
    }
  }

  private fun createCustomTestCase(
    tx: EntTransactionClient,
    customInputSubmissionId: Long,
    position: Int,
    input: JsonElement,
  ) {
    try {
      tx.customTestCases.create {
        this.customInputSubmissionId = customInputSubmissionId
        this.position = position
        inputJson = input
      }.save(ExecutionAccess.context).getOrThrow()
    } catch (exception: EntValidationException) {
      val violation = exception.violations.first()
      throw EnqueueCustomInputSubmissionInputException("cases[$position].${violation.field}", violation.message)
    }
  }

  private fun parseCaseInput(input: String, position: Int): JsonElement {
    val parsed = try {
      inputMapper.readTree(input)
    } catch (_: JacksonException) {
      throw EnqueueCustomInputSubmissionInputException("cases[$position].inputJson", "Provide valid JSON")
    }

    if (parsed == null || parsed.isMissingNode) {
      throw EnqueueCustomInputSubmissionInputException("cases[$position].inputJson", "Provide valid JSON")
    }

    // Keep the original numeric representation; normalizing 1.0 to 1 would change strict JSON comparisons.
    return Json.parseToJsonElement(input)
  }

  private fun createQueuedCustomInputSubmission(
    tx: EntTransactionClient,
    userId: Long,
    configurationId: Long,
    sourceCode: String,
    caseCount: Int,
  ): Long = tx.customInputSubmissions.create {
    this.userId = userId
    problemLanguageId = configurationId
    expiresAt = Instant.now().plus(properties.customInputSubmissionLifetime)
    this.sourceCode = sourceCode
    totalCases = caseCount
  }.saveAndLoad(ExecutionAccess.context).getOrThrow().id

  /** Loads the retained inputs through owner policies, independently of current problem availability. */
  fun findCustomInputSubmissionForCurrentUser(id: Long): CustomInputSubmission? {
    val user = currentUser.get() ?: return null
    val viewer = ViewerContext(Viewer.User(user.id))

    return entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      tx.customInputSubmissions.query {
        where(CustomInputSubmission.id eq id)
        loadCases { orderBy(CustomTestCase.position.asc()) }
      }.firstOrNull(viewer).visibleOrNull().getOrThrow()
    }.getOrThrow()
  }

  fun claimNextQueuedCustomInputSubmission(): CustomInputSubmission? = entClient.withTransaction { tx ->
    val customInputSubmission = tx.customInputSubmissions.indexes.status(CustomInputSubmissionStatus.QUEUED).query {
      orderBy(CustomInputSubmission.createdAt.asc())
      orderBy(CustomInputSubmission.id.asc())
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return@withTransaction null

    tx.customInputSubmissions.update(customInputSubmission.id) {
      status = CustomInputSubmissionStatus.RUNNING
      startedAt = Instant.now()
    }.saveAndLoad(ExecutionAccess.context).getOrThrow()
  }.getOrThrow()

  fun loadCustomInputs(customInputSubmissionId: Long): List<JsonElement> =
    entClient.customTestCases.indexes.customInputSubmissionId(customInputSubmissionId).query {
      orderBy(CustomTestCase.position.asc())
    }.all(ExecutionAccess.context).getOrThrow().map { it.inputJson }

  /** Saves all answers and the ready job together; a failed write leaves the custom input submission unprepared. */
  fun saveExpectedOutputsAndEnqueueGradingJob(customInputSubmissionId: Long, expectedOutputs: List<JsonElement>) {
    entClient.withTransaction { tx ->
      val customInputSubmission = lockRunningCustomInputSubmission(tx, customInputSubmissionId)
      val cases = loadCustomInputSubmissionCases(tx, customInputSubmissionId)

      check(cases.isNotEmpty() && cases.size == expectedOutputs.size) { "Expected outputs must match every custom input" }
      check(cases.all { it.expectedOutputJson == null }) { "Custom inputs are already prepared" }

      cases.forEachIndexed { index, testCase ->
        tx.customTestCases.update(testCase.id) {
          expectedOutputJson = expectedOutputs[index]
        }.save(ExecutionAccess.context).getOrThrow()
      }

      tx.gradingJobs.create {
        this.customInputSubmissionId = customInputSubmission.id
        problemLanguageId = customInputSubmission.problemLanguageId
        sourceCode = customInputSubmission.sourceCode
        this.cases = cases.mapIndexed { index, testCase ->
          GradingCase(testCase.id, testCase.inputJson, expectedOutputs[index])
        }
      }.save(ExecutionAccess.context).getOrThrow()
    }.getOrThrow()
  }

  /** Startup recovery only; a custom input submission with a job has completed reference preparation. */
  fun finishInterruptedCustomPreparations() {
    val customInputSubmissions = entClient.customInputSubmissions.indexes.status(CustomInputSubmissionStatus.RUNNING).query {
      loadGradingJob()
    }.all(ExecutionAccess.context).getOrThrow()

    for (customInputSubmission in customInputSubmissions) {
      if (customInputSubmission.edges.gradingJob.requireLoaded() != null) {
        continue
      }

      finishInterruptedCustomPreparation(customInputSubmission.id)
    }
  }

  /** Recovers one preparation owned by this scheduler after an uncertain write. */
  fun finishInterruptedCustomPreparation(customInputSubmissionId: Long) {
    entClient.withTransaction { tx ->
      val customInputSubmission = tx.customInputSubmissions.query {
        where(CustomInputSubmission.id eq customInputSubmissionId)
      }
        .forUpdate()
        .firstOrNull(ExecutionAccess.context)
        .getOrThrow() ?: return@withTransaction

      if (customInputSubmission.status != CustomInputSubmissionStatus.RUNNING) {
        return@withTransaction
      }

      val job = tx.gradingJobs.indexes.customInputSubmissionId(customInputSubmissionId).query()
        .firstOrNull(ExecutionAccess.context).getOrThrow()
      if (job != null) {
        return@withTransaction
      }

      finishCustomInputSubmission(
        tx, customInputSubmissionId, CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR),
      )
    }.getOrThrow()
  }

  /** Saves one summary and one result array; missing case results become NOT_RUN. */
  fun finishCustomInputSubmission(customInputSubmissionId: Long, result: CustomInputSubmissionResult) {
    entClient.withTransaction { tx ->
      finishCustomInputSubmission(tx, customInputSubmissionId, result)
    }.getOrThrow()
  }

  /** Joins job completion so retaining all custom results and deleting the job are atomic. */
  internal fun finishCustomInputSubmission(
    tx: EntTransactionClient,
    customInputSubmissionId: Long,
    result: CustomInputSubmissionResult,
  ) {
    val customInputSubmission = lockRunningCustomInputSubmission(tx, customInputSubmissionId)
    val cases = loadCustomInputSubmissionCases(tx, customInputSubmission.id)
    val caseResults = completeCaseResults(cases, result)

    tx.customInputSubmissions.update(customInputSubmissionId) {
      status = CustomInputSubmissionStatus.FINISHED
      outcome = result.outcome
      passedCases = caseResults.count { it.outcome == TestCaseOutcome.PASSED }
      this.caseResults = JsonArray(caseResults.map { it.toJson() })
      runtimeMs = result.runtimeMs
      publicErrorMessage = customInputSubmissionErrorMessage(result.outcome)
      finishedAt = Instant.now()
    }.save(ExecutionAccess.context).getOrThrow()
  }

  /** Deletes one batch of finished submissions whose expiration is at or before [cutoff]. */
  fun deleteExpiredCustomInputSubmissions(cutoff: Instant, batchSize: Int = 100): Int {
    require(batchSize > 0) { "Cleanup batch size must be positive" }

    return entClient.withTransaction { tx ->
      val submissions = tx.customInputSubmissions.indexes.expiresAt { lte(cutoff) }.query {
        where(CustomInputSubmission.status eq CustomInputSubmissionStatus.FINISHED)
        orderBy(CustomInputSubmission.expiresAt.asc())
        orderBy(CustomInputSubmission.id.asc())
        limit(batchSize)
      }
        .forUpdate()
        .all(ExecutionAccess.context)
        .getOrThrow()

      if (submissions.isEmpty()) {
        return@withTransaction 0
      }

      // Keep selection and deletion in one transaction; the foreign key deletes each submission's cases.
      tx.customInputSubmissions.deleteMany(
        ExecutionAccess.context,
        CustomInputSubmission.id `in` submissions.map { it.id },
      ).getOrThrow()
    }.getOrThrow()
  }

  private fun lockRunningCustomInputSubmission(
    tx: EntTransactionClient,
    customInputSubmissionId: Long,
  ): CustomInputSubmission {
    val customInputSubmission = tx.customInputSubmissions.query {
      where(CustomInputSubmission.id eq customInputSubmissionId)
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: error("Custom input submission is missing")

    check(customInputSubmission.status == CustomInputSubmissionStatus.RUNNING) {
      "Custom input submission is not running"
    }
    return customInputSubmission
  }

  private fun loadCustomInputSubmissionCases(
    tx: EntTransactionClient,
    customInputSubmissionId: Long,
  ): List<CustomTestCase> =
    tx.customTestCases.indexes.customInputSubmissionId(customInputSubmissionId).query {
      orderBy(CustomTestCase.position.asc())
    }.all(ExecutionAccess.context).getOrThrow()

  private fun completeCaseResults(
    cases: List<CustomTestCase>,
    result: CustomInputSubmissionResult,
  ): List<CustomTestCaseResult> {
    val resultsByCaseId = result.caseResults.associateBy { it.testCaseId }
    val caseIds = cases.map { it.id }.toSet()
    check(resultsByCaseId.size == result.caseResults.size) { "Duplicate custom case results" }
    check(caseIds.containsAll(resultsByCaseId.keys)) { "Results belong to another custom input submission" }
    check(result.runtimeMs == null || result.runtimeMs >= 0) { "Invalid custom input submission duration" }

    val completeResults = cases.map { testCase ->
      val caseResult = resultsByCaseId[testCase.id]
        ?: CustomTestCaseResult(testCase.id, TestCaseOutcome.NOT_RUN)

      // PostgreSQL JSONB rejects NUL characters. Retain only bounded user-program output.
      caseResult.copy(output = caseResult.output.replace('\u0000', '\uFFFD').take(20_000))
    }

    if (result.outcome == CustomInputSubmissionOutcome.PASSED) {
      check(completeResults.isNotEmpty() && completeResults.all { it.outcome == TestCaseOutcome.PASSED }) {
        "A passing custom input submission must pass every case"
      }
    }

    return completeResults
  }
}

package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.Submission
import com.application.ent.SubmissionFailure
import com.application.ent.TestCase
import com.application.execution.LanguageExecutionConfig
import com.application.execution.RuntimeAvailability
import com.application.execution.SubmissionExecutionResult
import com.application.execution.SubmissionExecutionSettings
import com.application.execution.TestSuiteResult
import com.application.schema.ProblemCheckerKind
import com.application.schema.SubmissionStatus
import com.application.schema.SubmissionTestOutcome
import com.application.schema.SubmissionTestSource
import com.application.schema.SubmissionVerdict
import com.application.security.CurrentUser
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SubmissionService(
  private val entClient: EntClient,
  private val currentUser: CurrentUser,
  private val properties: ExecutionProperties,
  languageExecutionConfigs: List<LanguageExecutionConfig>,
  private val runtimeAvailability: RuntimeAvailability,
) {
  private val languageConfigurations = languageExecutionConfigs.associateBy { it.key }
  private val publicContext = ViewerContext(Viewer.Anonymous)

  /** Validates the caller's source and atomically admits a persisted official submission. */
  fun submitSolution(
    problemLanguageId: Long?,
    sourceCode: String,
  ): SubmitSolutionOutcome {
    val user = currentUser.get() ?: return SubmitSolutionOutcome.AuthenticationRequired
    val configurationId = problemLanguageId ?: return SubmitSolutionOutcome.NotFound

    // Concurrent submissions may fail with a serialization error; revisit retries if contention becomes an issue.
    return entClient.withTransaction(IsolationLevel.Serializable) { tx ->
      enqueueSubmission(tx, user.id, configurationId, sourceCode)
    }.getOrThrow()
  }

  /**
   * Checks problem availability and queue capacity before saving the submission.
   * The serializable transaction prevents concurrent requests from both claiming the last queue slot.
   */
  private fun enqueueSubmission(
    tx: EntTransactionClient,
    userId: Long,
    configurationId: Long,
    sourceCode: String,
  ): SubmitSolutionOutcome {
    val configuration = tx.problemLanguages.findById(publicContext, configurationId)
      .visibleOrNull()
      .getOrThrow()
      ?: return SubmitSolutionOutcome.NotFound

    val problem = tx.problems.findById(publicContext, configuration.problemId)
      .visibleOrNull()
      .getOrThrow()
      ?: return SubmitSolutionOutcome.NotFound

    val language = tx.languages.findById(publicContext, configuration.languageId)
      .visibleOrNull()
      .getOrThrow()
      ?: return SubmitSolutionOutcome.NotFound

    if (!isProblemLanguageExecutable(tx, problem, configuration.id, language.key)) {
      return SubmitSolutionOutcome.Unavailable
    }

    if (!hasOfficialTestCases(tx, problem.id)) {
      return SubmitSolutionOutcome.Unavailable
    }

    if (!hasSubmissionCapacity(tx, userId)) {
      return SubmitSolutionOutcome.Busy
    }

    val submissionId = createQueuedSubmission(tx, userId, configuration, sourceCode)
    val submission = loadOwnedSubmission(tx, submissionId, userId)
      ?: error("Created submission could not be loaded")

    return SubmitSolutionOutcome.Success(submission)
  }

  private fun isProblemLanguageExecutable(
    tx: EntTransactionClient,
    problem: Problem,
    problemLanguageId: Long,
    languageKey: String,
  ): Boolean {
    if (languageKey !in languageConfigurations || problem.checkerKind != ProblemCheckerKind.EXACT_JSON) {
      return false
    }

    val judgeExists = tx.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
      .find(ExecutionAccess.context)
      .getOrThrow() != null

    if (!judgeExists) {
      return false
    }

    val configuredRuntime = properties.runtimes[languageKey]?.takeIf { it.isNotBlank() } ?: return false

    return runtimeAvailability.isAvailable(configuredRuntime)
  }

  private fun hasOfficialTestCases(tx: EntTransactionClient, problemId: Long): Boolean =
    tx.testCases.indexes.problemId(problemId).query {}
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow() != null

  /** Must run in the same serializable transaction that inserts the new submission. */
  private fun hasSubmissionCapacity(tx: EntTransactionClient, userId: Long): Boolean {
    val active = tx.submissions.query {
      where(Submission.status `in` listOf(SubmissionStatus.QUEUED, SubmissionStatus.RUNNING))
      limit(properties.maxActiveSubmissions)
    }
      .all(ExecutionAccess.context)
      .getOrThrow()

    return active.size < properties.maxActiveSubmissions &&
      active.count { it.userId == userId } < properties.maxActiveSubmissionsPerUser
  }

  private fun createQueuedSubmission(
    tx: EntTransactionClient,
    userId: Long,
    configuration: ProblemLanguage,
    sourceCode: String,
  ): Long {
    val submission = tx.submissions.create {
      this.userId = userId
      problemId = configuration.problemId
      problemLanguageId = configuration.id
      this.sourceCode = sourceCode
      // Set the count when the current official suite is selected at execution start.
      totalCases = 0
    }
      .saveAndLoad(ExecutionAccess.context)
      .getOrThrow()

    return submission.id
  }

  fun findSubmissionForCurrentUser(id: Long): Submission? {
    val user = currentUser.get() ?: return null

    return entClient.withTransaction { tx ->
      loadOwnedSubmission(tx, id, user.id)
    }.getOrThrow()
  }

  fun findFailedExampleForCurrentUser(submissionId: Long): SubmissionFailure? {
    val user = currentUser.get() ?: return null

    return entClient.withTransaction { tx ->
      loadOwnedSubmission(tx, submissionId, user.id) ?: return@withTransaction null

      tx.submissionFailures.indexes.submissionId(submissionId)
        .find(ViewerContext(Viewer.User(user.id)))
        .visibleOrNull()
        .getOrThrow()
    }.getOrThrow()
  }

  fun claimNextQueuedSubmission(): Submission? = entClient.withTransaction { tx ->
    val submission = tx.submissions.indexes.status(SubmissionStatus.QUEUED).query {
      orderBy(Submission.createdAt.asc())
      orderBy(Submission.id.asc())
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return@withTransaction null

    tx.submissions.update(submission.id) {
      status = SubmissionStatus.RUNNING
      startedAt = Instant.now()
    }.saveAndLoad(ExecutionAccess.context).getOrThrow()
  }.getOrThrow()

  fun loadExecutionSettings(submission: Submission): SubmissionExecutionSettings =
    entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      val configuration = tx.problemLanguages.findById(publicContext, submission.problemLanguageId)
        .getOrThrow() ?: error("Problem language is unavailable")

      val language = tx.languages.findById(publicContext, configuration.languageId)
        .getOrThrow() ?: error("Language is unavailable")

      val problem = tx.problems.findById(publicContext, submission.problemId)
        .getOrThrow() ?: error("Problem is unavailable")

      check(problem.checkerKind == ProblemCheckerKind.EXACT_JSON) { "Unsupported checker" }

      val judge = tx.judgeConfigurations.indexes.problemLanguageId(configuration.id)
        .find(ExecutionAccess.context)
        .getOrThrow() ?: error("Judge is unavailable")

      val languageConfiguration = languageConfigurations[language.key] ?: error("Unsupported language")
      val runtime = properties.runtimes[language.key]?.takeIf { it.isNotBlank() }
        ?: error("Runtime is unavailable")

      val cases = tx.testCases.indexes.problemId(problem.id).query {
        orderBy(TestCase.position.asc())
      }
        .all(ExecutionAccess.context)
        .getOrThrow()

      check(cases.isNotEmpty()) { "Official suite is empty" }

      tx.submissions.update(submission.id) {
        totalCases = cases.size
      }.save(ExecutionAccess.context).getOrThrow()

      SubmissionExecutionSettings(
        runtime = runtime,
        program = languageConfiguration.prepare(submission.sourceCode, judge.testDriverCode),
        timeLimitMs = judge.timeLimitMs,
        memoryLimitMb = judge.memoryLimitMb,
        cases = cases,
      )
    }.getOrThrow()

  fun finishSubmission(submissionId: Long, result: SubmissionExecutionResult) {
    entClient.withTransaction { tx ->
      val submission = tx.submissions.query { where(Submission.id eq submissionId) }
        .forUpdate()
        .firstOrNull(ExecutionAccess.context)
        .getOrThrow()
        ?: error("Submission is missing")

      check(submission.status == SubmissionStatus.RUNNING) { "Submission already finished" }

      val failedCase = result.failedCase
      val output = result.suiteResult
      if (failedCase != null && output != null) {
        saveFailedCase(tx, submissionId, failedCase, output, result.verdict)
      }

      tx.submissions.update(submissionId) {
        status = SubmissionStatus.FINISHED
        verdict = result.verdict
        passedCases = maxOf(submission.passedCases, result.passedCases)
        runtimeMs = result.runtimeMs ?: submission.runtimeMs
        publicErrorMessage = publicErrorMessage(result.verdict)
        finishedAt = Instant.now()
      }.save(ExecutionAccess.context).getOrThrow()
    }.getOrThrow()
  }

  private fun saveFailedCase(
    tx: EntTransactionClient,
    submissionId: Long,
    testCase: TestCase,
    result: TestSuiteResult,
    verdict: SubmissionVerdict,
  ) {
    tx.submissionFailures.create {
      this.submissionId = submissionId

      // Retain the selected input and visibility even if the original test changed during execution.
      source = SubmissionTestSource.valueOf(testCase.visibility.name)
      inputJson = testCase.inputJson
      expectedOutputJson = testCase.expectedOutputJson
      outcome = SubmissionTestOutcome.valueOf(verdict.name)

      // PostgreSQL text cannot contain NUL. Keep hidden diagnostics bounded and execution-only.
      stdout = result.stdout.replace('\u0000', '\uFFFD').take(20_000)
      stderr = result.stderr.replace('\u0000', '\uFFFD').take(20_000)
      // The executor measures the entire suite, so individual case timing remains unknown.
    }.save(ExecutionAccess.context).getOrThrow()
  }

  /** Called between attempts after runtime cleanup; only one scheduler may manage this database. */
  fun finishInterruptedSubmissions() {
    // RUNNING means a restart or a failed final write in this single-worker app.
    val interrupted = entClient.submissions.indexes.status(SubmissionStatus.RUNNING).query {}
      .all(ExecutionAccess.context)
      .getOrThrow()

    for (submission in interrupted) {
      finishSubmission(submission.id, SubmissionExecutionResult(SubmissionVerdict.INTERNAL_ERROR))
    }
  }

  private fun publicErrorMessage(verdict: SubmissionVerdict): String? = when (verdict) {
    SubmissionVerdict.COMPILE_ERROR -> "Compilation failed. Check the solution syntax and required function signature."
    SubmissionVerdict.RUNTIME_ERROR -> "The solution failed to return one JSON value within the output limit."
    SubmissionVerdict.TIME_LIMIT_EXCEEDED -> "Execution exceeded the time limit."
    SubmissionVerdict.MEMORY_LIMIT_EXCEEDED -> "Execution exceeded the memory limit."
    SubmissionVerdict.INTERNAL_ERROR -> "Execution could not complete. Submit the solution again."
    else -> null
  }

  private fun loadOwnedSubmission(
    tx: EntTransactionClient,
    id: Long,
    userId: Long,
  ): Submission? =
    tx.submissions.query {
      where(Submission.id eq id)
      where(Submission.userId eq userId)
    }
      .firstOrNull(ViewerContext(Viewer.User(userId)))
      .visibleOrNull()
      .getOrThrow()
}

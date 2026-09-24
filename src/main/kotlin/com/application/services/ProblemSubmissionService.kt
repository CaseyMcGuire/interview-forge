package com.application.services

import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.ProblemLanguage
import com.application.ent.ProblemSubmission
import com.application.ent.ProblemSubmissionFailure
import com.application.ent.TestCase
import com.application.execution.GradingResult
import com.application.execution.TestCaseGradingResult
import com.application.execution.TestCaseOutcome
import com.application.execution.toProblemSubmissionVerdict
import com.application.execution.toProblemSubmissionTestOutcome
import com.application.schema.GradingCase
import com.application.schema.ProblemSubmissionStatus
import com.application.schema.ProblemSubmissionTestSource
import com.application.schema.ProblemSubmissionVerdict
import com.application.security.CurrentUserService
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ProblemSubmissionService(
  private val entClient: EntClient,
  private val currentUserService: CurrentUserService,
  private val executionAvailabilityService: ExecutionAvailabilityService,
) {
  private val publicContext = ViewerContext(Viewer.Anonymous)

  /** Validates the caller's source and atomically admits a persisted official submission. */
  fun submitSolution(
    problemLanguageId: Long?,
    sourceCode: String,
  ): SubmitSolutionOutcome {
    val user = currentUserService.get() ?: return SubmitSolutionOutcome.AuthenticationRequired
    val configurationId = problemLanguageId ?: return SubmitSolutionOutcome.NotFound

    // Concurrent submissions may fail with a serialization error; revisit retries if contention becomes an issue.
    return entClient.withTransaction(IsolationLevel.Serializable) { tx ->
      enqueueProblemSubmission(tx, user.id, configurationId, sourceCode)
    }.getOrThrow()
  }

  /**
   * Checks problem availability and queue capacity before saving the submission.
   * The serializable transaction prevents concurrent requests from both claiming the last queue slot.
   */
  private fun enqueueProblemSubmission(
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

    val judge = executionAvailabilityService.findExecutableJudgeConfiguration(tx, problem, configuration.id, language.key)
    if (judge == null) {
      return SubmitSolutionOutcome.Unavailable
    }

    val cases = loadOfficialGradingCases(tx, problem.id)
    if (cases.isEmpty()) {
      return SubmitSolutionOutcome.Unavailable
    }

    if (!executionAvailabilityService.hasExecutionCapacity(tx, userId)) {
      return SubmitSolutionOutcome.Busy
    }

    val problemSubmissionId = createQueuedProblemSubmission(tx, userId, configuration, sourceCode, cases)
    val problemSubmission = loadOwnedProblemSubmission(tx, problemSubmissionId, userId)
      ?: error("Created submission could not be loaded")

    return SubmitSolutionOutcome.Success(problemSubmission)
  }

  private fun loadOfficialGradingCases(tx: EntTransactionClient, problemId: Long): List<GradingCase> =
    tx.testCases.indexes.problemId(problemId).query {
      orderBy(TestCase.position.asc())
      orderBy(TestCase.id.asc())
    }
      .all(ExecutionAccess.context)
      .getOrThrow()
      .map { GradingCase(it.id, it.inputJson, it.expectedOutputJson, it.visibility) }

  /** Saves the attempt and its selected cases as a grading job in the admission transaction. */
  private fun createQueuedProblemSubmission(
    tx: EntTransactionClient,
    userId: Long,
    configuration: ProblemLanguage,
    sourceCode: String,
    cases: List<GradingCase>,
  ): Long {
    val problemSubmission = tx.problemSubmissions.create {
      this.userId = userId
      problemId = configuration.problemId
      problemLanguageId = configuration.id
      this.sourceCode = sourceCode
      totalCases = cases.size
    }
      .saveAndLoad(ExecutionAccess.context)
      .getOrThrow()

    tx.gradingJobs.create {
      problemSubmissionId = problemSubmission.id
      problemLanguageId = configuration.id
      this.sourceCode = sourceCode
      this.cases = cases
    }.save(ExecutionAccess.context).getOrThrow()

    return problemSubmission.id
  }

  fun findProblemSubmissionForCurrentUser(id: Long): ProblemSubmission? {
    val user = currentUserService.get() ?: return null

    return entClient.withTransaction { tx ->
      loadOwnedProblemSubmission(tx, id, user.id)
    }.getOrThrow()
  }

  fun findFailedExampleForCurrentUser(problemSubmissionId: Long): ProblemSubmissionFailure? {
    val user = currentUserService.get() ?: return null

    return entClient.withTransaction { tx ->
      loadOwnedProblemSubmission(tx, problemSubmissionId, user.id) ?: return@withTransaction null

      tx.problemSubmissionFailures.indexes.problemSubmissionId(problemSubmissionId)
        .find(ViewerContext(Viewer.User(user.id)))
        .visibleOrNull()
        .getOrThrow()
    }.getOrThrow()
  }

  /** Shares the job-completion transaction so its result and job deletion commit together. */
  internal fun finishProblemSubmission(
    tx: EntTransactionClient,
    problemSubmissionId: Long,
    cases: List<GradingCase>,
    result: GradingResult,
  ) {
    // Use a query here because completion must lock the row before checking its state.
    val problemSubmission = tx.problemSubmissions.query { where(ProblemSubmission.id eq problemSubmissionId) }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: error("Problem submission is missing")

    check(problemSubmission.status == ProblemSubmissionStatus.RUNNING) { "Problem submission is not running" }
    check(result.caseResults.size == cases.size) { "Grading results must match the selected cases" }

    val failedCaseIndex = result.caseResults.indexOfFirst {
      it.outcome != TestCaseOutcome.PASSED && it.outcome != TestCaseOutcome.NOT_RUN
    }
    if (failedCaseIndex >= 0) {
      saveFailedCase(tx, problemSubmissionId, cases[failedCaseIndex], result.caseResults[failedCaseIndex])
    }

    val verdict = result.outcome.toProblemSubmissionVerdict()
    tx.problemSubmissions.update(problemSubmissionId) {
      status = ProblemSubmissionStatus.FINISHED
      this.verdict = verdict
      passedCases = result.passedCases
      runtimeMs = result.runtimeMs
      publicErrorMessage = publicErrorMessage(verdict)
      finishedAt = Instant.now()
    }.save(ExecutionAccess.context).getOrThrow()
  }

  private fun saveFailedCase(
    tx: EntTransactionClient,
    problemSubmissionId: Long,
    testCase: GradingCase,
    result: TestCaseGradingResult,
  ) {
    val execution = checkNotNull(result.execution) { "A failed case must have an execution result" }

    tx.problemSubmissionFailures.create {
      this.problemSubmissionId = problemSubmissionId

      // Retain the selected input and visibility even if the original test changed during execution.
      source = ProblemSubmissionTestSource.valueOf(checkNotNull(testCase.visibility).name)
      inputJson = testCase.inputJson
      expectedOutputJson = testCase.expectedOutputJson
      outcome = result.outcome.toProblemSubmissionTestOutcome()

      // PostgreSQL text cannot contain NUL. Keep hidden diagnostics bounded and execution-only.
      stdout = execution.stdout.replace('\u0000', '\uFFFD').take(20_000)
      stderr = execution.stderr.replace('\u0000', '\uFFFD').take(20_000)
      runtimeMs = execution.runtimeMs
    }.save(ExecutionAccess.context).getOrThrow()
  }

  private fun publicErrorMessage(verdict: ProblemSubmissionVerdict): String? = when (verdict) {
    ProblemSubmissionVerdict.COMPILE_ERROR -> "Compilation failed. Check the solution syntax and required function signature."
    ProblemSubmissionVerdict.RUNTIME_ERROR -> "The solution failed to return one JSON value within the output limit."
    ProblemSubmissionVerdict.TIME_LIMIT_EXCEEDED -> "Execution exceeded the time limit."
    ProblemSubmissionVerdict.MEMORY_LIMIT_EXCEEDED -> "Execution exceeded the memory limit."
    ProblemSubmissionVerdict.INTERNAL_ERROR -> "Execution could not complete. Submit the solution again."
    else -> null
  }

  private fun loadOwnedProblemSubmission(
    tx: EntTransactionClient,
    id: Long,
    userId: Long,
  ): ProblemSubmission? =
    tx.problemSubmissions.query {
      where(ProblemSubmission.id eq id)
      where(ProblemSubmission.userId eq userId)
    }
      .firstOrNull(ViewerContext(Viewer.User(userId)))
      .visibleOrNull()
      .getOrThrow()
}

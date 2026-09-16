package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.Submission
import com.application.execution.LanguageExecutionConfig
import com.application.execution.RuntimeAvailability
import com.application.schema.ProblemCheckerKind
import com.application.schema.SubmissionKind
import com.application.schema.SubmissionStatus
import com.application.security.CurrentUser
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class SubmissionService(
  private val entClient: EntClient,
  private val currentUser: CurrentUser,
  private val properties: ExecutionProperties,
  languageExecutionConfigs: List<LanguageExecutionConfig>,
  private val runtimeAvailability: ObjectProvider<RuntimeAvailability>,
) {
  private val supportedLanguageKeys = languageExecutionConfigs.map { it.key }.toSet()
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
    if (languageKey !in supportedLanguageKeys || problem.checkerKind != ProblemCheckerKind.EXACT_JSON) {
      return false
    }

    val judgeExists = tx.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
      .find(ExecutionAccess.context)
      .getOrThrow() != null

    if (!judgeExists) {
      return false
    }

    val configuredRuntime = properties.runtimes[languageKey]?.takeIf { it.isNotBlank() } ?: return false

    return runtimeAvailability.ifAvailable?.isAvailable(configuredRuntime) == true
  }

  private fun hasOfficialTestCases(tx: EntTransactionClient, problemId: Long): Boolean =
    tx.testCases.indexes.problemId(problemId).query {}
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow() != null

  /** Must run in the same serializable transaction that inserts the new submission. */
  private fun hasSubmissionCapacity(tx: EntTransactionClient, userId: Long): Boolean {
    val active = tx.submissions.query {
      where(Submission.kind eq SubmissionKind.SUBMIT)
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
      kind = SubmissionKind.SUBMIT
      // The worker selects the current official suite and sets the count when execution starts.
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

  private fun loadOwnedSubmission(
    tx: EntTransactionClient,
    id: Long,
    userId: Long,
  ): Submission? =
    tx.submissions.query {
      where(Submission.id eq id)
      where(Submission.userId eq userId)
      where(Submission.kind eq SubmissionKind.SUBMIT)
    }
      .firstOrNull(ViewerContext(Viewer.User(userId)))
      .visibleOrNull()
      .getOrThrow()
}

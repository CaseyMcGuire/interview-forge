package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.CustomTestSuiteRun
import com.application.ent.EntTransactionClient
import com.application.ent.JudgeConfiguration
import com.application.ent.Problem
import com.application.ent.Submission
import com.application.execution.LanguageExecutionConfig
import com.application.execution.RuntimeAvailability
import com.application.schema.CustomTestSuiteRunStatus
import com.application.schema.ProblemCheckerKind
import com.application.schema.SubmissionStatus
import com.application.security.ExecutionAccess
import entkt.runtime.query.requireLoaded
import org.springframework.stereotype.Service

/** Availability and shared queue limits for official submissions and custom test suite runs. */
@Service
class ExecutionAvailabilityService(
  private val properties: ExecutionProperties,
  languageExecutionConfigs: List<LanguageExecutionConfig>,
  private val runtimeAvailability: RuntimeAvailability,
) {
  private val supportedLanguages = languageExecutionConfigs.map { it.key }.toSet()

  fun findExecutableJudgeConfiguration(
    tx: EntTransactionClient,
    problem: Problem,
    problemLanguageId: Long,
    languageKey: String,
  ): JudgeConfiguration? {
    if (languageKey !in supportedLanguages || problem.checkerKind != ProblemCheckerKind.EXACT_JSON) {
      return null
    }

    val judge = tx.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
      .find(ExecutionAccess.context)
      .getOrThrow()
      ?: return null

    val runtime = properties.runtimes[languageKey]?.takeIf { it.isNotBlank() } ?: return null
    if (!runtimeAvailability.isAvailable(runtime)) {
      return null
    }

    return judge
  }

  /** Must share the serializable transaction that inserts either kind of attempt. */
  fun hasExecutionCapacity(tx: EntTransactionClient, userId: Long): Boolean {
    val submissions = tx.submissions.query {
      where(Submission.status `in` listOf(SubmissionStatus.QUEUED, SubmissionStatus.RUNNING))
      limit(properties.maxActiveSubmissions)
    }.all(ExecutionAccess.context).getOrThrow()

    if (submissions.size >= properties.maxActiveSubmissions) {
      return false
    }

    val customRuns = tx.customTestSuiteRuns.query {
      where(CustomTestSuiteRun.status `in` listOf(CustomTestSuiteRunStatus.QUEUED, CustomTestSuiteRunStatus.RUNNING))
      limit(properties.maxActiveSubmissions)
      loadCustomTestSuite()
    }.all(ExecutionAccess.context).getOrThrow()

    val activeCount = submissions.size + customRuns.size
    val userActiveCount = submissions.count { it.userId == userId } + customRuns.count {
      checkNotNull(it.edges.customTestSuite.requireLoaded()).userId == userId
    }

    return activeCount < properties.maxActiveSubmissions && userActiveCount < properties.maxActiveSubmissionsPerUser
  }
}

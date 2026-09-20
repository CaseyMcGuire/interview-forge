package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.CustomInputSubmission
import com.application.ent.EntTransactionClient
import com.application.ent.JudgeConfiguration
import com.application.ent.Problem
import com.application.ent.ProblemSubmission
import com.application.execution.LanguageExecutionConfig
import com.application.execution.RuntimeAvailability
import com.application.schema.CustomInputSubmissionStatus
import com.application.schema.ProblemCheckerKind
import com.application.schema.ProblemSubmissionStatus
import com.application.security.ExecutionAccess
import org.springframework.stereotype.Service

/** Availability and shared queue limits for problem submissions and custom input submissions. */
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
    val problemSubmissions = tx.problemSubmissions.query {
      where(ProblemSubmission.status `in` listOf(ProblemSubmissionStatus.QUEUED, ProblemSubmissionStatus.RUNNING))
      limit(properties.maxActiveSubmissions)
    }.all(ExecutionAccess.context).getOrThrow()

    if (problemSubmissions.size >= properties.maxActiveSubmissions) {
      return false
    }

    val customInputSubmissions = tx.customInputSubmissions.query {
      where(CustomInputSubmission.status `in` listOf(CustomInputSubmissionStatus.QUEUED, CustomInputSubmissionStatus.RUNNING))
      limit(properties.maxActiveSubmissions)
    }.all(ExecutionAccess.context).getOrThrow()

    val activeCount = problemSubmissions.size + customInputSubmissions.size
    val userActiveCount = problemSubmissions.count { it.userId == userId } + customInputSubmissions.count { it.userId == userId }

    return activeCount < properties.maxActiveSubmissions && userActiveCount < properties.maxActiveSubmissionsPerUser
  }
}

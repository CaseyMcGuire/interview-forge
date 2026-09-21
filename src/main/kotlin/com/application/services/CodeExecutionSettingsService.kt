package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.JudgeConfiguration
import com.application.execution.CodeExecutionSettings
import com.application.execution.LanguageExecutionConfig
import com.application.schema.ProblemCheckerKind
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import org.springframework.stereotype.Service

/** Prepares source files and commands using current language and judge configuration. */
@Service
class CodeExecutionSettingsService(
  private val entClient: EntClient,
  private val properties: ExecutionProperties,
  languageExecutionConfigs: List<LanguageExecutionConfig>,
) {
  private val languageConfigurations = languageExecutionConfigs.associateBy { it.key }
  private val publicContext = ViewerContext(Viewer.Anonymous)

  fun loadSubmittedCodeSettings(problemLanguageId: Long, sourceCode: String): CodeExecutionSettings =
    entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      val judge = findJudgeConfiguration(tx, problemLanguageId)
        ?: error("Judge is unavailable")

      prepareExecutionSettings(tx, problemLanguageId, judge, sourceCode)
        ?: error("Execution configuration is unavailable")
    }.getOrThrow()

  fun loadReferenceSolutionSettings(problemLanguageId: Long): CodeExecutionSettings =
    findReferenceSolutionSettings(problemLanguageId) ?: error("Reference solution is unavailable")

  /** Returns null when the problem, language, reference solution, or configured runtime is unavailable. */
  fun findReferenceSolutionSettings(problemLanguageId: Long): CodeExecutionSettings? =
    entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      val judge = findJudgeConfiguration(tx, problemLanguageId) ?: return@withTransaction null
      val sourceCode = judge.referenceSolutionCode?.takeIf { it.isNotBlank() }
        ?: return@withTransaction null

      prepareExecutionSettings(tx, problemLanguageId, judge, sourceCode)
    }.getOrThrow()

  private fun findJudgeConfiguration(tx: EntTransactionClient, problemLanguageId: Long): JudgeConfiguration? =
    tx.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
      .find(ExecutionAccess.context)
      .getOrThrow()

  private fun prepareExecutionSettings(
    tx: EntTransactionClient,
    problemLanguageId: Long,
    judge: JudgeConfiguration,
    sourceCode: String,
  ): CodeExecutionSettings? {
    val configuration = tx.problemLanguages.findById(publicContext, problemLanguageId)
      .visibleOrNull()
      .getOrThrow() ?: return null

    val language = tx.languages.findById(publicContext, configuration.languageId)
      .visibleOrNull()
      .getOrThrow() ?: return null

    val problem = tx.problems.findById(publicContext, configuration.problemId)
      .visibleOrNull()
      .getOrThrow() ?: return null

    if (problem.checkerKind != ProblemCheckerKind.EXACT_JSON) {
      return null
    }

    val languageConfiguration = languageConfigurations[language.key] ?: return null
    val runtime = properties.runtimes[language.key]?.takeIf { it.isNotBlank() }
      ?: return null

    return CodeExecutionSettings(
      runtime = runtime,
      program = languageConfiguration.prepare(sourceCode, judge.testDriverCode),
      timeLimitMs = judge.timeLimitMs,
      memoryLimitMb = judge.memoryLimitMb,
    )
  }
}

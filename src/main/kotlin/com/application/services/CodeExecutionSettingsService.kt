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
      val judge = loadJudgeConfiguration(tx, problemLanguageId)
      prepareExecutionSettings(tx, problemLanguageId, judge, sourceCode)
    }.getOrThrow()

  fun loadReferenceSolutionSettings(problemLanguageId: Long): CodeExecutionSettings =
    entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      val judge = loadJudgeConfiguration(tx, problemLanguageId)
      val sourceCode = judge.referenceSolutionCode?.takeIf { it.isNotBlank() }
        ?: error("Reference solution is unavailable")

      prepareExecutionSettings(tx, problemLanguageId, judge, sourceCode)
    }.getOrThrow()

  private fun loadJudgeConfiguration(tx: EntTransactionClient, problemLanguageId: Long): JudgeConfiguration =
    tx.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
      .find(ExecutionAccess.context)
      .getOrThrow() ?: error("Judge is unavailable")

  private fun prepareExecutionSettings(
    tx: EntTransactionClient,
    problemLanguageId: Long,
    judge: JudgeConfiguration,
    sourceCode: String,
  ): CodeExecutionSettings {
    val configuration = tx.problemLanguages.findById(publicContext, problemLanguageId)
      .getOrThrow() ?: error("Problem language is unavailable")

    val language = tx.languages.findById(publicContext, configuration.languageId)
      .getOrThrow() ?: error("Language is unavailable")

    val problem = tx.problems.findById(publicContext, configuration.problemId)
      .getOrThrow() ?: error("Problem is unavailable")
    check(problem.checkerKind == ProblemCheckerKind.EXACT_JSON) { "Unsupported checker" }

    val languageConfiguration = languageConfigurations[language.key] ?: error("Unsupported language")
    val runtime = properties.runtimes[language.key]?.takeIf { it.isNotBlank() }
      ?: error("Runtime is unavailable")

    return CodeExecutionSettings(
      runtime = runtime,
      program = languageConfiguration.prepare(sourceCode, judge.testDriverCode),
      timeLimitMs = judge.timeLimitMs,
      memoryLimitMb = judge.memoryLimitMb,
    )
  }
}

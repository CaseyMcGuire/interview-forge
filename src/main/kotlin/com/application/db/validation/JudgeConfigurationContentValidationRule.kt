package com.application.db.validation

import com.application.ent.JudgeConfigurationCreateValidationRule
import com.application.ent.JudgeConfigurationWriteCandidate
import com.application.ent.ReadOnlyEntClient
import com.application.schema.ProblemCheckerKind
import entkt.runtime.result.visibleOrNull
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class JudgeConfigurationContentValidationRule : JudgeConfigurationCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: JudgeConfigurationWriteCandidate,
  ): ValidationDecision = when {
    item.runtime.isBlank() || item.runtime.length > 100 ->
      ValidationDecision.Invalid("Runtime is required and must be at most 100 characters", field = "runtime")

    item.testDriverCode.isBlank() || item.testDriverCode.length > 50_000 ->
      ValidationDecision.Invalid(
        "Test driver code is required and must be at most 50,000 characters",
        field = "testDriverCode",
      )

    (item.checkerSource?.length ?: 0) > 50_000 ->
      ValidationDecision.Invalid("Checker code must be at most 50,000 characters", field = "checkerSource")

    item.timeLimitMs !in 1..60_000 ->
      ValidationDecision.Invalid("Time limit must be between 1 and 60,000 milliseconds", field = "timeLimitMs")

    item.memoryLimitMb !in 1..8_192 ->
      ValidationDecision.Invalid("Memory limit must be between 1 and 8,192 megabytes", field = "memoryLimitMb")

    else -> validateChecker(context, item)
  }

  /** Whether a checker belongs depends on the owning problem, so this is the only database lookup. */
  private fun validateChecker(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: JudgeConfigurationWriteCandidate,
  ): ValidationDecision {
    val configuration = context.client.problemLanguages
      .findById(context.readViewerContext, item.problemLanguageId)
      .visibleOrNull()
      .getOrThrow()
    val problem = configuration?.let {
      context.client.problems.findById(context.readViewerContext, it.problemId).visibleOrNull().getOrThrow()
    } ?: return ValidationDecision.Invalid("The language configuration is unavailable", field = "problemLanguageId")

    return when {
      problem.checkerKind == ProblemCheckerKind.CUSTOM && item.checkerSource.isNullOrBlank() ->
        ValidationDecision.Invalid("Checker code is required for problems with a custom checker", field = "checkerSource")

      problem.checkerKind == ProblemCheckerKind.EXACT_JSON && item.checkerSource != null ->
        ValidationDecision.Invalid("Checker code is only used by problems with a custom checker", field = "checkerSource")

      else -> ValidationDecision.Valid
    }
  }
}

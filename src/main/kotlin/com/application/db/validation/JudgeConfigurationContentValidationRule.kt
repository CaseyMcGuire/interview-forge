package com.application.db.validation

import com.application.ent.JudgeConfigurationCreateValidationRule
import com.application.ent.JudgeConfigurationWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class JudgeConfigurationContentValidationRule : JudgeConfigurationCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: JudgeConfigurationWriteCandidate,
  ): ValidationDecision = when {
    item.testDriverCode.isBlank() || item.testDriverCode.length > 50_000 ->
      ValidationDecision.Invalid(
        "Test driver code is required and must be at most 50,000 characters",
        field = "testDriverCode",
      )

    item.referenceSolutionCode?.isBlank() == true || (item.referenceSolutionCode?.length ?: 0) > 50_000 ->
      ValidationDecision.Invalid(
        "Reference solution code must be nonblank and at most 50,000 characters",
        field = "referenceSolutionCode",
      )

    item.timeLimitMs !in 1..60_000 ->
      ValidationDecision.Invalid("Time limit must be between 1 and 60,000 milliseconds", field = "timeLimitMs")

    item.memoryLimitMb !in 1..8_192 ->
      ValidationDecision.Invalid("Memory limit must be between 1 and 8,192 megabytes", field = "memoryLimitMb")

    else -> ValidationDecision.Valid
  }
}

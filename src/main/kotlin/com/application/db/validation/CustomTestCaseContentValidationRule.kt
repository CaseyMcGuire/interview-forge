package com.application.db.validation

import com.application.ent.CustomTestCaseCreateValidationRule
import com.application.ent.CustomTestCaseWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class CustomTestCaseContentValidationRule : CustomTestCaseCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: CustomTestCaseWriteCandidate,
  ): ValidationDecision = when {
    item.position < 0 ->
      ValidationDecision.Invalid("Position must be nonnegative", field = "position")

    item.inputJson.toString().length > 20_000 ->
      ValidationDecision.Invalid("Input must be at most 20,000 characters", field = "inputJson")

    (item.expectedOutputJson?.toString()?.length ?: 0) > 20_000 ->
      ValidationDecision.Invalid("Expected output must be at most 20,000 characters", field = "expectedOutputJson")

    else -> ValidationDecision.Valid
  }
}

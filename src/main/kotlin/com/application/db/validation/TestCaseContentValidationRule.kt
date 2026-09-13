package com.application.db.validation

import com.application.ent.ReadOnlyEntClient
import com.application.ent.TestCaseCreateValidationRule
import com.application.ent.TestCaseWriteCandidate
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class TestCaseContentValidationRule : TestCaseCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: TestCaseWriteCandidate,
  ): ValidationDecision = when {
    item.inputJson.toString().length > 20_000 ->
      ValidationDecision.Invalid("Input must be at most 20,000 characters", field = "inputJson")

    item.expectedOutputJson.toString().length > 20_000 ->
      ValidationDecision.Invalid("Expected output must be at most 20,000 characters", field = "expectedOutputJson")

    (item.explanationMarkdown?.length ?: 0) > 10_000 ->
      ValidationDecision.Invalid("Explanation must be at most 10,000 characters", field = "explanationMarkdown")

    else -> ValidationDecision.Valid
  }
}

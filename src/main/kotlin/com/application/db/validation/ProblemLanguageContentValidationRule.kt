package com.application.db.validation

import com.application.ent.ProblemLanguageCreateValidationRule
import com.application.ent.ProblemLanguageWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class ProblemLanguageContentValidationRule : ProblemLanguageCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: ProblemLanguageWriteCandidate,
  ): ValidationDecision = if (item.starterCode.isBlank() || item.starterCode.length > 50_000) {
    ValidationDecision.Invalid(
      "Starter code is required and must be at most 50,000 characters",
      field = "starterCode",
    )
  } else {
    ValidationDecision.Valid
  }
}

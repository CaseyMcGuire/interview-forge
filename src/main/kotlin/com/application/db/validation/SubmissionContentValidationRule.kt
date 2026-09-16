package com.application.db.validation

import com.application.ent.ReadOnlyEntClient
import com.application.ent.SubmissionCreateValidationRule
import com.application.ent.SubmissionWriteCandidate
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

object SubmissionContentValidationRule : SubmissionCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: SubmissionWriteCandidate,
  ): ValidationDecision {
    val sourceCode = item.sourceCode

    return if (sourceCode.isBlank() || sourceCode.length > 50_000) {
      ValidationDecision.Invalid(
        "Provide nonblank source of at most 50,000 characters",
        field = "sourceCode",
      )
    } else {
      ValidationDecision.Valid
    }
  }
}

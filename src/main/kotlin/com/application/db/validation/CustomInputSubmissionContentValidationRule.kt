package com.application.db.validation

import com.application.ent.CustomInputSubmissionCreateValidationRule
import com.application.ent.CustomInputSubmissionWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class CustomInputSubmissionContentValidationRule : CustomInputSubmissionCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: CustomInputSubmissionWriteCandidate,
  ): ValidationDecision = validateSourceCode(item.sourceCode)
}

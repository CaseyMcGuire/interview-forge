package com.application.db.validation

import com.application.ent.ReadOnlyEntClient
import com.application.ent.ProblemSubmissionCreateValidationRule
import com.application.ent.ProblemSubmissionWriteCandidate
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

object ProblemSubmissionContentValidationRule : ProblemSubmissionCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: ProblemSubmissionWriteCandidate,
  ): ValidationDecision = validateSourceCode(item.sourceCode)
}

package com.application.db.validation

import com.application.ent.GradingJobCreateValidationRule
import com.application.ent.GradingJobWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class GradingJobContentValidationRule : GradingJobCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: GradingJobWriteCandidate,
  ): ValidationDecision {
    when {
      item.problemSubmissionId == null && item.customInputSubmissionId == null ->
        return ValidationDecision.Invalid("A grading job must belong to a problem submission or a custom input submission")

      item.problemSubmissionId != null && item.customInputSubmissionId != null ->
        return ValidationDecision.Invalid("A grading job cannot belong to both a problem submission and a custom input submission")

      item.cases.isEmpty() ->
        return ValidationDecision.Invalid("A grading job must contain at least one case", field = "cases")
    }

    return validateSourceCode(item.sourceCode)
  }
}

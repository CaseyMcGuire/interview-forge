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
      item.submissionId == null && item.customTestSuiteRunId == null ->
        return ValidationDecision.Invalid("A grading job must belong to a submission or a custom test suite run")

      item.submissionId != null && item.customTestSuiteRunId != null ->
        return ValidationDecision.Invalid("A grading job cannot belong to both a submission and a custom test suite run")

      item.cases.isEmpty() ->
        return ValidationDecision.Invalid("A grading job must contain at least one case", field = "cases")
    }

    return validateSourceCode(item.sourceCode)
  }
}

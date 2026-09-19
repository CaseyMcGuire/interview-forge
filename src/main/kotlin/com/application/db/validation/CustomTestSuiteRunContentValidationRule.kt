package com.application.db.validation

import com.application.ent.CustomTestSuiteRunCreateValidationRule
import com.application.ent.CustomTestSuiteRunWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class CustomTestSuiteRunContentValidationRule : CustomTestSuiteRunCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: CustomTestSuiteRunWriteCandidate,
  ): ValidationDecision = validateSourceCode(item.sourceCode)
}

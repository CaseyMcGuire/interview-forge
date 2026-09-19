package com.application.db.policies

import com.application.db.policies.rules.AllowIfCustomTestCaseOwnerReadRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.CustomTestCaseContentValidationRule
import com.application.ent.CustomTestCase
import com.application.ent.CustomTestCasePolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class CustomTestCasePolicy : EntityPolicy<CustomTestCase, CustomTestCasePolicyScope> {
  override fun configure(scope: CustomTestCasePolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), AllowIfCustomTestCaseOwnerReadRule())
      create(AllowIfExecutionRule())
      update(AllowIfExecutionRule())
    }

    validation {
      create(CustomTestCaseContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

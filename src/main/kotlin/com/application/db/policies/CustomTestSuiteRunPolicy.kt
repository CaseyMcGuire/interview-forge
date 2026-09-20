package com.application.db.policies

import com.application.db.policies.rules.AllowIfCustomTestSuiteRunOwnerReadRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.CustomTestSuiteRunContentValidationRule
import com.application.ent.CustomTestSuiteRun
import com.application.ent.CustomTestSuiteRunPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class CustomTestSuiteRunPolicy(
  private val ownerReadRule: AllowIfCustomTestSuiteRunOwnerReadRule,
) : EntityPolicy<CustomTestSuiteRun, CustomTestSuiteRunPolicyScope> {
  override fun configure(scope: CustomTestSuiteRunPolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), ownerReadRule)
      create(AllowIfExecutionRule())
      update(AllowIfExecutionRule())
    }

    validation {
      create(CustomTestSuiteRunContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

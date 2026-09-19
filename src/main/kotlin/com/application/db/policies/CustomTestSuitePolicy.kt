package com.application.db.policies

import com.application.db.policies.rules.AllowIfCustomTestSuiteOwnerReadRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.ent.CustomTestSuite
import com.application.ent.CustomTestSuitePolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class CustomTestSuitePolicy(
  private val ownerReadRule: AllowIfCustomTestSuiteOwnerReadRule,
) : EntityPolicy<CustomTestSuite, CustomTestSuitePolicyScope> {
  override fun configure(scope: CustomTestSuitePolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), ownerReadRule)
      create(AllowIfExecutionRule())
    }
  }
}

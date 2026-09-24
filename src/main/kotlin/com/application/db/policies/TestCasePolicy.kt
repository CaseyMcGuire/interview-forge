package com.application.db.policies

import com.application.db.policies.rules.AllowIfAvailableTestCaseReadRule
import com.application.db.policies.rules.AllowIfAdminRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.TestCaseContentValidationRule
import com.application.ent.TestCase
import com.application.ent.TestCasePolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class TestCasePolicy(
  private val adminRule: AllowIfAdminRule,
  private val testCaseReadRule: AllowIfAvailableTestCaseReadRule,
) : EntityPolicy<TestCase, TestCasePolicyScope> {
  override fun configure(scope: TestCasePolicyScope) = scope.run {
    privacy {
      create(adminRule)
      update(adminRule)
      delete(adminRule)
      load(AllowIfExecutionRule(), testCaseReadRule)
    }

    validation {
      create(TestCaseContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

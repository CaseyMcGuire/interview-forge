package com.application.db.policies

import com.application.db.policies.rules.AllowIfPublicExampleReadRule
import com.application.db.policies.rules.AllowIfAdminCreateRule
import com.application.db.validation.TestCaseContentValidationRule
import com.application.ent.TestCase
import com.application.ent.TestCasePolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class TestCasePolicy(
  private val adminCreateRule: AllowIfAdminCreateRule,
) : EntityPolicy<TestCase, TestCasePolicyScope> {
  override fun configure(scope: TestCasePolicyScope) = scope.run {
    privacy {
      create(adminCreateRule)
      load(AllowIfPublicExampleReadRule())
    }

    validation {
      create(TestCaseContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

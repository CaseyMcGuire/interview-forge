package com.application.db.policies

import com.application.db.policies.rules.AllowIfPublicExampleReadRule
import com.application.ent.TestCase
import com.application.ent.TestCasePolicyScope
import entkt.runtime.privacy.EntityPolicy

object TestCasePolicy : EntityPolicy<TestCase, TestCasePolicyScope> {
  override fun configure(scope: TestCasePolicyScope) = scope.run {
    privacy {
      load(AllowIfPublicExampleReadRule())
    }
  }
}

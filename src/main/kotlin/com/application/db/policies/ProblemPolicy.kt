package com.application.db.policies

import com.application.db.policies.rules.AllowIfPublishedReadRule
import com.application.db.policies.rules.AllowIfAdminCreateRule
import com.application.db.validation.ProblemContentValidationRule
import com.application.ent.Problem
import com.application.ent.ProblemPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class ProblemPolicy(
  private val adminCreateRule: AllowIfAdminCreateRule,
) : EntityPolicy<Problem, ProblemPolicyScope> {
  override fun configure(scope: ProblemPolicyScope) = scope.run {
    privacy {
      create(adminCreateRule)
      load(AllowIfPublishedReadRule())
    }

    validation {
      create(ProblemContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

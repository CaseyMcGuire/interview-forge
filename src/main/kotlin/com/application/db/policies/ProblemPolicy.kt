package com.application.db.policies

import com.application.db.policies.rules.AllowIfPublishedReadRule
import com.application.db.policies.rules.AllowIfAdminRule
import com.application.db.policies.rules.DenyIfProblemUnavailableUpdateRule
import com.application.db.validation.ProblemContentValidationRule
import com.application.ent.Problem
import com.application.ent.ProblemPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class ProblemPolicy(
  private val adminRule: AllowIfAdminRule,
) : EntityPolicy<Problem, ProblemPolicyScope> {
  override fun configure(scope: ProblemPolicyScope) = scope.run {
    privacy {
      create(adminRule)
      update(DenyIfProblemUnavailableUpdateRule())
      update(adminRule)
      load(AllowIfPublishedReadRule())
    }

    validation {
      create(ProblemContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

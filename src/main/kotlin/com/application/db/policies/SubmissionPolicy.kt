package com.application.db.policies

import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.policies.rules.AllowIfSubmissionOwnerReadRule
import com.application.db.validation.SubmissionContentValidationRule
import com.application.ent.Submission
import com.application.ent.SubmissionPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class SubmissionPolicy(
  private val ownerReadRule: AllowIfSubmissionOwnerReadRule,
) : EntityPolicy<Submission, SubmissionPolicyScope> {

  override fun configure(scope: SubmissionPolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), ownerReadRule)
      create(AllowIfExecutionRule())
      update(AllowIfExecutionRule())
    }

    validation {
      create(SubmissionContentValidationRule)
      updateDerivesFromCreate()
    }
  }
}

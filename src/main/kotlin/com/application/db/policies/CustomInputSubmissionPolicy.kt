package com.application.db.policies

import com.application.db.policies.rules.AllowIfCustomInputSubmissionOwnerReadRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.CustomInputSubmissionContentValidationRule
import com.application.ent.CustomInputSubmission
import com.application.ent.CustomInputSubmissionPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class CustomInputSubmissionPolicy(
  private val ownerReadRule: AllowIfCustomInputSubmissionOwnerReadRule,
) : EntityPolicy<CustomInputSubmission, CustomInputSubmissionPolicyScope> {
  override fun configure(scope: CustomInputSubmissionPolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), ownerReadRule)
      create(AllowIfExecutionRule())
      update(AllowIfExecutionRule())
    }

    validation {
      create(CustomInputSubmissionContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

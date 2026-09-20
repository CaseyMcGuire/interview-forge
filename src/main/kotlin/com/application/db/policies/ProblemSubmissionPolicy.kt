package com.application.db.policies

import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.policies.rules.AllowIfProblemSubmissionOwnerReadRule
import com.application.db.validation.ProblemSubmissionContentValidationRule
import com.application.ent.ProblemSubmission
import com.application.ent.ProblemSubmissionPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class ProblemSubmissionPolicy(
  private val ownerReadRule: AllowIfProblemSubmissionOwnerReadRule,
) : EntityPolicy<ProblemSubmission, ProblemSubmissionPolicyScope> {

  override fun configure(scope: ProblemSubmissionPolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), ownerReadRule)
      create(AllowIfExecutionRule())
      update(AllowIfExecutionRule())
    }

    validation {
      create(ProblemSubmissionContentValidationRule)
      updateDerivesFromCreate()
    }
  }
}

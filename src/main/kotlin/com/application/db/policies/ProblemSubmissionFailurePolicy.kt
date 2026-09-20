package com.application.db.policies

import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.policies.rules.AllowIfFailedExampleOwnerReadRule
import com.application.ent.ProblemSubmissionFailure
import com.application.ent.ProblemSubmissionFailurePolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class ProblemSubmissionFailurePolicy : EntityPolicy<ProblemSubmissionFailure, ProblemSubmissionFailurePolicyScope> {
  override fun configure(scope: ProblemSubmissionFailurePolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), AllowIfFailedExampleOwnerReadRule())
      create(AllowIfExecutionRule())
    }
  }
}

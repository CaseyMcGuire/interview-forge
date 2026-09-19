package com.application.db.policies

import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.policies.rules.AllowIfFailedExampleOwnerReadRule
import com.application.ent.SubmissionFailure
import com.application.ent.SubmissionFailurePolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class SubmissionFailurePolicy : EntityPolicy<SubmissionFailure, SubmissionFailurePolicyScope> {
  override fun configure(scope: SubmissionFailurePolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule(), AllowIfFailedExampleOwnerReadRule())
      create(AllowIfExecutionRule())
    }
  }
}

package com.application.db.policies

import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.ent.SubmissionTestResult
import com.application.ent.SubmissionTestResultPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class SubmissionTestResultPolicy : EntityPolicy<SubmissionTestResult, SubmissionTestResultPolicyScope> {
  override fun configure(scope: SubmissionTestResultPolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule())
      create(AllowIfExecutionRule())
    }
  }
}

package com.application.db.policies

import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.GradingJobContentValidationRule
import com.application.ent.GradingJob
import com.application.ent.GradingJobPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class GradingJobPolicy : EntityPolicy<GradingJob, GradingJobPolicyScope> {
  override fun configure(scope: GradingJobPolicyScope) = scope.run {
    privacy {
      load(AllowIfExecutionRule())
      create(AllowIfExecutionRule())
      update(AllowIfExecutionRule())
      delete(AllowIfExecutionRule())
    }

    validation {
      create(GradingJobContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

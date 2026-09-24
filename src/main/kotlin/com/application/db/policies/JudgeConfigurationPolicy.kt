package com.application.db.policies

import com.application.db.policies.rules.AllowIfAdminRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.JudgeConfigurationContentValidationRule
import com.application.ent.JudgeConfiguration
import com.application.ent.JudgeConfigurationPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

/** Judge settings are private: the parent problem and language availability is checked by callers. */
@Component
class JudgeConfigurationPolicy(
  private val adminRule: AllowIfAdminRule,
) : EntityPolicy<JudgeConfiguration, JudgeConfigurationPolicyScope> {
  override fun configure(scope: JudgeConfigurationPolicyScope) = scope.run {
    privacy {
      create(adminRule)
      update(adminRule)
      load(AllowIfExecutionRule())
      load(adminRule)
    }

    validation {
      create(JudgeConfigurationContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

package com.application.db.policies

import com.application.db.policies.rules.AllowIfAdminCreateRule
import com.application.db.policies.rules.AllowIfAdminReadRule
import com.application.db.policies.rules.AllowIfAdminUpdateRule
import com.application.db.policies.rules.AllowIfExecutionRule
import com.application.db.validation.JudgeConfigurationContentValidationRule
import com.application.ent.JudgeConfiguration
import com.application.ent.JudgeConfigurationPolicyScope
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

/** Judge settings are private: the parent problem and language availability is checked by callers. */
@Component
class JudgeConfigurationPolicy(
  private val adminCreateRule: AllowIfAdminCreateRule,
  private val adminUpdateRule: AllowIfAdminUpdateRule,
  private val adminReadRule: AllowIfAdminReadRule,
) : EntityPolicy<JudgeConfiguration, JudgeConfigurationPolicyScope> {
  override fun configure(scope: JudgeConfigurationPolicyScope) = scope.run {
    privacy {
      create(adminCreateRule)
      update(adminUpdateRule)
      load(AllowIfExecutionRule(), adminReadRule)
    }

    validation {
      create(JudgeConfigurationContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

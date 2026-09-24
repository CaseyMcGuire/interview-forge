package com.application.db.policies

import com.application.db.policies.rules.AllowIfProblemAndLanguageAvailableReadRule
import com.application.db.policies.rules.AllowIfAdminRule
import com.application.db.validation.EnabledProblemLanguageValidationRule
import com.application.db.validation.ProblemLanguageContentValidationRule
import com.application.ent.ProblemLanguage
import com.application.ent.ProblemLanguagePolicyScope
import com.application.ent.ProblemLanguageUpdateValidationRule
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class ProblemLanguagePolicy(
  private val adminRule: AllowIfAdminRule,
) : EntityPolicy<ProblemLanguage, ProblemLanguagePolicyScope> {
  private val contentValidationRule = ProblemLanguageContentValidationRule()

  override fun configure(scope: ProblemLanguagePolicyScope) = scope.run {
    privacy {
      create(adminRule)
      update(adminRule)
      load(AllowIfProblemAndLanguageAvailableReadRule())
    }

    validation {
      create(contentValidationRule, EnabledProblemLanguageValidationRule())
      update(ProblemLanguageUpdateValidationRule { context, item ->
        contentValidationRule.validate(context, item.candidate)
      })
    }
  }
}

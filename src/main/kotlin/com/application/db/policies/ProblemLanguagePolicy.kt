package com.application.db.policies

import com.application.db.policies.rules.AllowIfProblemAndLanguageAvailableReadRule
import com.application.ent.ProblemLanguage
import com.application.ent.ProblemLanguagePolicyScope
import entkt.runtime.privacy.EntityPolicy

object ProblemLanguagePolicy : EntityPolicy<ProblemLanguage, ProblemLanguagePolicyScope> {
  override fun configure(scope: ProblemLanguagePolicyScope) = scope.run {
    privacy {
      load(AllowIfProblemAndLanguageAvailableReadRule())
    }
  }
}

package com.application.db.policies

import com.application.db.policies.rules.AllowIfLanguageEnabledReadRule
import com.application.ent.Language
import com.application.ent.LanguagePolicyScope
import entkt.runtime.privacy.EntityPolicy

object LanguagePolicy : EntityPolicy<Language, LanguagePolicyScope> {
  override fun configure(scope: LanguagePolicyScope) = scope.run {
    privacy {
      load(AllowIfLanguageEnabledReadRule())
    }
  }
}

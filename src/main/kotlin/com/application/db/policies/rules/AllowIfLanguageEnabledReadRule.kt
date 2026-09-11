package com.application.db.policies.rules

import com.application.ent.Language
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext

class AllowIfLanguageEnabledReadRule : PrivacyRule<ReadOnlyEntClient, Language> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: Language): PrivacyDecision =
    if (item.enabled) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Language is disabled")
    }
}

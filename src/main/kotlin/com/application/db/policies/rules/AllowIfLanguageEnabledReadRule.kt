package com.application.db.policies.rules

import com.application.ent.Language
import com.application.ent.LanguageLoadPrivacyRule
import com.application.ent.ReadOnlyEntClient
import com.application.security.AppViewer
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext

class AllowIfLanguageEnabledReadRule : LanguageLoadPrivacyRule {
  override fun run(
    context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
    item: Language,
  ): PrivacyDecision =
    if (item.enabled) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Language is disabled")
    }
}

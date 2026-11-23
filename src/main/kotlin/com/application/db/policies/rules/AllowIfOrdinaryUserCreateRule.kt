package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.ent.UserCreatePrivacyRule
import com.application.ent.UserWriteCandidate
import com.application.schema.UserRole
import com.application.security.AppViewer
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext

class AllowIfOrdinaryUserCreateRule : UserCreatePrivacyRule {
  override fun run(
    context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
    item: UserWriteCandidate,
  ): PrivacyDecision =
    if (item.role == UserRole.USER) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Registration cannot grant administrator access")
    }
}

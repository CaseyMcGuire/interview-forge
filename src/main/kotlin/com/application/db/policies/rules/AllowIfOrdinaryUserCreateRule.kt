package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.ent.UserWriteCandidate
import com.application.schema.UserRole
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext

class AllowIfOrdinaryUserCreateRule : PrivacyRule<ReadOnlyEntClient, UserWriteCandidate> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: UserWriteCandidate): PrivacyDecision =
    if (item.role == UserRole.USER) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Registration cannot grant administrator access")
    }
}

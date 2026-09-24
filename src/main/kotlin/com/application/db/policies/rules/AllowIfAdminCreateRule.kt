package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.security.CurrentUserService
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import org.springframework.stereotype.Component

@Component
class AllowIfAdminCreateRule(private val currentUserService: CurrentUserService) : PrivacyRule<ReadOnlyEntClient, Any> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: Any): PrivacyDecision =
    if (currentUserService.isAdmin(context.viewerContext, context.client)) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Only administrators can create problem content")
    }
}

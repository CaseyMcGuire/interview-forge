package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.security.CurrentUserService
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import org.springframework.stereotype.Component

/** For private admin content such as judge configurations; only the viewer is checked. */
@Component
class AllowIfAdminReadRule(private val currentUserService: CurrentUserService) : PrivacyRule<ReadOnlyEntClient, Any> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: Any): PrivacyDecision =
    if (currentUserService.isAdmin(context.viewerContext, context.client)) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Only administrators can read this content")
    }
}

package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.security.AppViewer
import com.application.security.CurrentUserService
import entkt.runtime.privacy.ContextPrivacyRule
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import org.springframework.stereotype.Component

@Component
class AllowIfAdminRule(
  private val currentUserService: CurrentUserService,
) : ContextPrivacyRule<AppViewer, ReadOnlyEntClient> {
  override fun run(context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>): PrivacyDecision =
    if (currentUserService.isAdmin(context.viewer, context.client)) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Administrator access is required")
    }
}

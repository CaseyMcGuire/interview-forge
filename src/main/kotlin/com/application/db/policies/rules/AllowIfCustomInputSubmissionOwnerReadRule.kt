package com.application.db.policies.rules

import com.application.ent.CustomInputSubmission
import com.application.ent.CustomInputSubmissionLoadPrivacyRule
import com.application.ent.ReadOnlyEntClient
import com.application.security.AppViewer
import com.application.security.CurrentUserService
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import org.springframework.stereotype.Component

@Component
class AllowIfCustomInputSubmissionOwnerReadRule(
  private val currentUserService: CurrentUserService,
) : CustomInputSubmissionLoadPrivacyRule {
  override fun run(
    context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
    item: CustomInputSubmission,
  ): PrivacyDecision {
    val user = currentUserService.getForViewer(context.viewer, context.client)

    return if (user?.id == item.userId) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Custom test suite run is unavailable to this viewer")
    }
  }
}

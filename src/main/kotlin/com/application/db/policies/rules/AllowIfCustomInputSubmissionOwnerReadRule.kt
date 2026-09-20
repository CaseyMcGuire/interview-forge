package com.application.db.policies.rules

import com.application.ent.CustomInputSubmission
import com.application.ent.ReadOnlyEntClient
import com.application.security.CurrentUser
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.privacy.longIdOrNull
import org.springframework.stereotype.Component

@Component
class AllowIfCustomInputSubmissionOwnerReadRule(
  private val currentUser: CurrentUser,
) : PrivacyRule<ReadOnlyEntClient, CustomInputSubmission> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: CustomInputSubmission): PrivacyDecision {
    val user = currentUser.get()

    return if (
      user != null &&
      user.id == context.viewerContext.longIdOrNull() &&
      user.id == item.userId
    ) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Custom test suite run is unavailable to this viewer")
    }
  }
}

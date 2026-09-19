package com.application.db.policies.rules

import com.application.ent.CustomTestSuite
import com.application.ent.ReadOnlyEntClient
import com.application.security.CurrentUser
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.privacy.longIdOrNull
import org.springframework.stereotype.Component

@Component
class AllowIfCustomTestSuiteOwnerReadRule(
  private val currentUser: CurrentUser,
) : PrivacyRule<ReadOnlyEntClient, CustomTestSuite> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: CustomTestSuite): PrivacyDecision {
    val user = currentUser.get()

    return if (
      user != null &&
      user.id == context.viewerContext.longIdOrNull() &&
      user.id == item.userId
    ) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Custom test suite is unavailable to this viewer")
    }
  }
}

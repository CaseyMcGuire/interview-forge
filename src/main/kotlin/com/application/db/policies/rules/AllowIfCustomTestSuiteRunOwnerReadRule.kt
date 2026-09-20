package com.application.db.policies.rules

import com.application.ent.CustomTestSuiteRun
import com.application.ent.ReadOnlyEntClient
import com.application.security.CurrentUser
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.privacy.longIdOrNull
import org.springframework.stereotype.Component

@Component
class AllowIfCustomTestSuiteRunOwnerReadRule(
  private val currentUser: CurrentUser,
) : PrivacyRule<ReadOnlyEntClient, CustomTestSuiteRun> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: CustomTestSuiteRun): PrivacyDecision {
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

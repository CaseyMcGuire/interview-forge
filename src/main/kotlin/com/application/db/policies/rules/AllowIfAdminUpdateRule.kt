package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.security.CurrentUser
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import org.springframework.stereotype.Component

/**
 * Shared by Problem, ProblemLanguage, and TestCase updates; only the viewer is checked.
 * Their update inputs differ, but PrivacyRule's `in Item` lets a rule accepting Any
 * safely satisfy each typed update slot. The item is intentionally unused.
 * Kotlin's Any is a common supertype, so this does not bypass type checking.
 */
@Component
class AllowIfAdminUpdateRule(private val currentUser: CurrentUser) : PrivacyRule<ReadOnlyEntClient, Any> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: Any): PrivacyDecision =
    if (currentUser.isAdmin(context.viewerContext)) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Only administrators can update problem content")
    }
}

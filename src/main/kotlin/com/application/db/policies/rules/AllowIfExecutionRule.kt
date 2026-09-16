package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.security.ExecutionAccess
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.privacy.Viewer

internal class AllowIfExecutionRule : PrivacyRule<ReadOnlyEntClient, Any> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: Any): PrivacyDecision {
    val viewer = context.viewerContext.viewer

    return if (viewer is Viewer.User && viewer.id === ExecutionAccess) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Continue
    }
  }
}

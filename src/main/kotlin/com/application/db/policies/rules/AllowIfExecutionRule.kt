package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.security.AppViewer
import com.application.security.ExecutionAccess
import entkt.runtime.privacy.ContextPrivacyRule
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext

internal class AllowIfExecutionRule : ContextPrivacyRule<AppViewer, ReadOnlyEntClient> {
  override fun run(context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>): PrivacyDecision =
    if (context.viewer === ExecutionAccess) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Continue
    }
}

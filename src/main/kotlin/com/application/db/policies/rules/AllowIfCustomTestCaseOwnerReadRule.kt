package com.application.db.policies.rules

import com.application.ent.CustomTestCase
import com.application.ent.CustomTestCaseLoadPrivacyRule
import com.application.ent.ReadOnlyEntClient
import com.application.security.AppViewer
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.result.visibleOrNull

class AllowIfCustomTestCaseOwnerReadRule : CustomTestCaseLoadPrivacyRule {
  override fun run(
    context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
    item: CustomTestCase,
  ): PrivacyDecision {
    // The run's policy enforces authenticated ownership, including after problem archival.
    context.client.customInputSubmissions.findById(context.viewerContext, item.customInputSubmissionId)
      .visibleOrNull()
      .getOrThrow()
      ?: return PrivacyDecision.Continue

    return PrivacyDecision.Allow
  }
}

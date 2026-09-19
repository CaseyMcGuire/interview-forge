package com.application.db.policies.rules

import com.application.ent.CustomTestCase
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.result.visibleOrNull

class AllowIfCustomTestCaseOwnerReadRule : PrivacyRule<ReadOnlyEntClient, CustomTestCase> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: CustomTestCase): PrivacyDecision {
    // The suite's policy enforces authenticated ownership, including after problem archival.
    context.client.customTestSuites.findById(context.viewerContext, item.customTestSuiteId)
      .visibleOrNull()
      .getOrThrow()
      ?: return PrivacyDecision.Continue

    return PrivacyDecision.Allow
  }
}

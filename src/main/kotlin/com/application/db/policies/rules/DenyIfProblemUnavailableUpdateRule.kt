package com.application.db.policies.rules

import com.application.ent.ProblemUpdatePrivacyRule
import com.application.ent.ProblemUpdateRuleInput
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import java.time.Instant

class DenyIfProblemUnavailableUpdateRule : ProblemUpdatePrivacyRule {
  override fun run(
    context: PrivacyRuleContext<ReadOnlyEntClient>,
    item: ProblemUpdateRuleInput,
  ): PrivacyDecision {
    val publishedAt = item.before.publishedAt
    return if (publishedAt == null || publishedAt.isAfter(Instant.now()) || item.before.archivedAt != null) {
      PrivacyDecision.Deny("Problem is not published or has been archived")
    } else {
      PrivacyDecision.Continue
    }
  }
}

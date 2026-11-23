package com.application.db.policies.rules

import com.application.ent.Problem
import com.application.ent.ProblemLoadPrivacyRule
import com.application.ent.ReadOnlyEntClient
import com.application.security.AppViewer
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import java.time.Instant

class AllowIfPublishedReadRule : ProblemLoadPrivacyRule {
  override fun run(
    context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
    item: Problem,
  ): PrivacyDecision {
    val publishedAt = item.publishedAt
    return if (publishedAt != null && !publishedAt.isAfter(Instant.now()) && item.archivedAt == null) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Problem is not published or has been archived")
    }
  }
}

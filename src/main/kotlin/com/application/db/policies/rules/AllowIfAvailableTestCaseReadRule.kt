package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.ent.TestCase
import com.application.schema.TestCaseVisibility
import com.application.security.CurrentUser
import entkt.runtime.privacy.BatchPrivacyRule
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.privacy.allowAll
import entkt.runtime.rule.RuleBatch
import entkt.runtime.rule.RuleDecisions
import org.springframework.stereotype.Component

@Component
class AllowIfAvailableTestCaseReadRule(
  private val currentUser: CurrentUser,
) : BatchPrivacyRule<ReadOnlyEntClient, TestCase> {
  override fun runBatch(
    context: PrivacyRuleContext<ReadOnlyEntClient>,
    batch: RuleBatch<TestCase>,
  ): RuleDecisions<PrivacyDecision> {
    if (currentUser.isAdmin(context.viewerContext)) {
      return batch.allowAll()
    }

    val problemIds = publishedProblemIds(
      context,
      batch.filter { it.visibility == TestCaseVisibility.EXAMPLE }.map { it.problemId },
    )

    return batch.decideEach {
      if (it.visibility == TestCaseVisibility.EXAMPLE && it.problemId in problemIds) {
        PrivacyDecision.Allow
      } else {
        PrivacyDecision.Deny("Test case is unavailable to this viewer")
      }
    }
  }
}

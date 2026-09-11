package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.ent.TestCase
import com.application.schema.TestCaseVisibility
import entkt.runtime.privacy.BatchPrivacyRule
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.rule.RuleBatch
import entkt.runtime.rule.RuleDecisions

class AllowIfPublicExampleReadRule : BatchPrivacyRule<ReadOnlyEntClient, TestCase> {
  override fun runBatch(
    context: PrivacyRuleContext<ReadOnlyEntClient>,
    batch: RuleBatch<TestCase>,
  ): RuleDecisions<PrivacyDecision> {
    val problemIds = publishedProblemIds(
      context,
      batch.filter { it.visibility == TestCaseVisibility.EXAMPLE }.map { it.problemId },
    )

    return batch.decideEach {
      if (it.visibility == TestCaseVisibility.EXAMPLE && it.problemId in problemIds) {
        PrivacyDecision.Allow
      } else {
        PrivacyDecision.Deny("Test case is not a public example of an available problem")
      }
    }
  }
}

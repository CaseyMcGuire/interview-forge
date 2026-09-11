package com.application.db.policies.rules

import com.application.ent.Language
import com.application.ent.ProblemLanguage
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.privacy.BatchPrivacyRule
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.rule.RuleBatch
import entkt.runtime.rule.RuleDecisions

class AllowIfProblemAndLanguageAvailableReadRule : BatchPrivacyRule<ReadOnlyEntClient, ProblemLanguage> {
  override fun runBatch(
    context: PrivacyRuleContext<ReadOnlyEntClient>,
    batch: RuleBatch<ProblemLanguage>,
  ): RuleDecisions<PrivacyDecision> {
    val problemIds = publishedProblemIds(context, batch.map { it.problemId })
    val languageIds = context.client.languages.query {
      where(Language.id `in` batch.map { it.languageId }.distinct())
      where(Language.enabled eq true)
    }.all(context.viewerContext).getOrThrow().map { it.id }.toSet()

    return batch.decideEach {
      if (it.problemId in problemIds && it.languageId in languageIds) {
        PrivacyDecision.Allow
      } else {
        PrivacyDecision.Deny("Problem or language is unavailable")
      }
    }
  }
}

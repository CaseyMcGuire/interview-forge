package com.application.db.policies.rules

import com.application.ent.ReadOnlyEntClient
import com.application.ent.SubmissionFailure
import com.application.schema.SubmissionStatus
import com.application.schema.SubmissionTestOutcome
import com.application.schema.SubmissionTestSource
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRule
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.result.visibleOrNull

/** Uses retained visibility so making a hidden test public never reveals an earlier hidden failure. */
class AllowIfFailedExampleOwnerReadRule : PrivacyRule<ReadOnlyEntClient, SubmissionFailure> {
  override fun run(context: PrivacyRuleContext<ReadOnlyEntClient>, item: SubmissionFailure): PrivacyDecision {
    if (item.source != SubmissionTestSource.EXAMPLE) {
      return PrivacyDecision.Continue
    }

    if (item.outcome in listOf(
      SubmissionTestOutcome.PENDING,
      SubmissionTestOutcome.PASSED,
      SubmissionTestOutcome.EXECUTED,
      SubmissionTestOutcome.SKIPPED,
    )) {
      return PrivacyDecision.Continue
    }

    // The submission's policy enforces authenticated ownership.
    val submission = context.client.submissions.findById(context.viewerContext, item.submissionId)
      .visibleOrNull()
      .getOrThrow()
      ?: return PrivacyDecision.Continue

    if (submission.status != SubmissionStatus.FINISHED) {
      return PrivacyDecision.Continue
    }

    return PrivacyDecision.Allow
  }
}

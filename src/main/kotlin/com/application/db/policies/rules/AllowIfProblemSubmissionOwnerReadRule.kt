package com.application.db.policies.rules

import com.application.ent.ProblemSubmissionLoadPrivacyRule
import com.application.ent.ReadOnlyEntClient
import com.application.ent.ProblemSubmission
import com.application.security.AppViewer
import com.application.security.CurrentUserService
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import org.springframework.stereotype.Component

@Component
class AllowIfProblemSubmissionOwnerReadRule(
  private val currentUserService: CurrentUserService,
) : ProblemSubmissionLoadPrivacyRule {

  override fun run(
    context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
    item: ProblemSubmission,
  ): PrivacyDecision {
    val user = currentUserService.getForViewer(context.viewer, context.client)

    return if (user?.id == item.userId) {
      PrivacyDecision.Allow
    } else {
      PrivacyDecision.Deny("Problem submission is unavailable to this viewer")
    }
  }
}

package com.application.db.policies

import com.application.db.policies.rules.AllowIfPublishedReadRule
import com.application.ent.Problem
import com.application.ent.ProblemPolicyScope
import entkt.runtime.privacy.EntityPolicy

object ProblemPolicy : EntityPolicy<Problem, ProblemPolicyScope> {
  override fun configure(scope: ProblemPolicyScope) = scope.run {
    privacy {
      load(AllowIfPublishedReadRule())
    }
  }
}

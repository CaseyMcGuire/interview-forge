package com.application.db.policies

import com.application.ent.User
import com.application.ent.UserPolicyScope
import com.application.db.policies.rules.AllowIfOrdinaryUserCreateRule
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class UserPolicy : EntityPolicy<User, UserPolicyScope> {
  override fun configure(scope: UserPolicyScope) = scope.run {
    privacy {
      // Registration is public. Credential reads, updates, and deletes remain denied by default.
      create(AllowIfOrdinaryUserCreateRule())
    }
  }
}

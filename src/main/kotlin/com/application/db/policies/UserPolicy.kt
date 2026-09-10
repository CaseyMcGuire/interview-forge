package com.application.db.policies

import com.application.ent.User
import com.application.ent.UserPolicyScope
import entkt.runtime.privacy.EntityPolicy
import entkt.runtime.privacy.allowAll

object UserPolicy : EntityPolicy<User, UserPolicyScope> {
  override fun configure(scope: UserPolicyScope) = scope.run {
    privacy {
      // Registration is public. Credential reads, updates, and deletes remain denied by default.
      create(allowAll)
    }
  }
}

package com.application.db.policies

import com.application.ent.User
import com.application.ent.UserPolicyScope
import com.application.db.policies.rules.AllowIfOrdinaryUserCreateRule
import com.application.security.CurrentUserService.AllowIfCurrentUserLookupReadRule
import entkt.runtime.privacy.EntityPolicy
import org.springframework.stereotype.Component

@Component
class UserPolicy : EntityPolicy<User, UserPolicyScope> {
  override fun configure(scope: UserPolicyScope) = scope.run {
    privacy {
      // Registration is public; ordinary viewers cannot read credentials or modify accounts.
      create(AllowIfOrdinaryUserCreateRule())
      load(AllowIfCurrentUserLookupReadRule())
    }
  }
}

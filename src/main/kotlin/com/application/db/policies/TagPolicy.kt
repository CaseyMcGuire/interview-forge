package com.application.db.policies

import com.application.db.policies.rules.AllowIfAdminRule
import com.application.db.validation.TagContentValidationRule
import com.application.ent.Tag
import com.application.ent.TagPolicyScope
import entkt.runtime.privacy.EntityPolicy
import entkt.runtime.privacy.allowAll
import org.springframework.stereotype.Component

@Component
class TagPolicy(
  private val adminRule: AllowIfAdminRule,
) : EntityPolicy<Tag, TagPolicyScope> {
  override fun configure(scope: TagPolicyScope) = scope.run {
    privacy {
      load(allowAll)
      create(adminRule)
      update(adminRule)
      delete(adminRule)
    }

    validation {
      create(TagContentValidationRule())
      updateDerivesFromCreate()
    }
  }
}

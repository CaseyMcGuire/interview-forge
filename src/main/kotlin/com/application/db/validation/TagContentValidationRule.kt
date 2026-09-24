package com.application.db.validation

import com.application.ent.ReadOnlyEntClient
import com.application.ent.TagCreateValidationRule
import com.application.ent.TagWriteCandidate
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class TagContentValidationRule : TagCreateValidationRule {
  private val slugPattern = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: TagWriteCandidate,
  ): ValidationDecision = when {
    item.slug.length !in 1..100 || !slugPattern.matches(item.slug) ->
      ValidationDecision.Invalid(
        "Tag slug must contain 1–100 characters using lowercase letters, numbers, and single hyphens",
        field = "slug",
      )

    item.displayName.isBlank() || item.displayName.length > 100 ->
      ValidationDecision.Invalid("Tag display name must contain 1–100 characters", field = "displayName")

    item.displayName != item.displayName.trim() ->
      ValidationDecision.Invalid("Tag display name must be trimmed", field = "displayName")

    else -> ValidationDecision.Valid
  }
}

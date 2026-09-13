package com.application.db.validation

import com.application.ent.ProblemCreateValidationRule
import com.application.ent.ProblemWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class ProblemContentValidationRule : ProblemCreateValidationRule {
  private val slugPattern = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: ProblemWriteCandidate,
  ): ValidationDecision = when {
    item.slug.length !in 1..100 || !slugPattern.matches(item.slug) || item.slug == "create" ->
      ValidationDecision.Invalid(
        "Slug must use lowercase letters, numbers, and single hyphens, and cannot be 'create'",
        field = "slug",
      )

    item.title.isBlank() || item.title.length > 200 ->
      ValidationDecision.Invalid("Title must contain 1–200 characters", field = "title")

    item.statementMarkdown.isBlank() || item.statementMarkdown.length > 100_000 ->
      ValidationDecision.Invalid(
        "Statement is required and must be at most 100,000 characters",
        field = "statementMarkdown",
      )

    else -> ValidationDecision.Valid
  }
}

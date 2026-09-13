package com.application.db.validation

import com.application.ent.ProblemLanguageCreateValidationRule
import com.application.ent.ProblemLanguageWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class ProblemLanguageContentValidationRule : ProblemLanguageCreateValidationRule {
  private val filenamePattern = Regex("[A-Za-z0-9_][A-Za-z0-9_.-]*")

  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: ProblemLanguageWriteCandidate,
  ): ValidationDecision = when {
    item.starterCode.isBlank() || item.starterCode.length > 50_000 ->
      ValidationDecision.Invalid(
        "Starter code is required and must be at most 50,000 characters",
        field = "starterCode",
      )

    item.solutionFilename.length !in 1..255 || !filenamePattern.matches(item.solutionFilename) ->
      ValidationDecision.Invalid(
        "Provide a filename without directory separators",
        field = "solutionFilename",
      )

    else -> ValidationDecision.Valid
  }
}

package com.application.db.validation

import com.application.ent.ProblemLanguageCreateValidationRule
import com.application.ent.ProblemLanguageWriteCandidate
import com.application.ent.ReadOnlyEntClient
import entkt.runtime.validation.ValidationDecision
import entkt.runtime.validation.ValidationRuleContext

class EnabledProblemLanguageValidationRule : ProblemLanguageCreateValidationRule {
  override fun validate(
    context: ValidationRuleContext<ReadOnlyEntClient>,
    item: ProblemLanguageWriteCandidate,
  ): ValidationDecision {
    val language = context.client.languages
      .findById(context.readViewerContext, item.languageId)
      .getOrThrow()

    return if (language?.enabled == true) {
      ValidationDecision.Valid
    } else {
      ValidationDecision.Invalid(
        "Every configuration must use an enabled language from the catalog",
        field = "languageId",
      )
    }
  }
}

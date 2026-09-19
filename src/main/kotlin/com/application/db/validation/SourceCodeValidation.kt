package com.application.db.validation

import entkt.runtime.validation.ValidationDecision

internal fun validateSourceCode(sourceCode: String): ValidationDecision =
  if (sourceCode.isBlank() || sourceCode.length > 50_000) {
    ValidationDecision.Invalid(
      "Provide nonblank source of at most 50,000 characters",
      field = "sourceCode",
    )
  } else {
    ValidationDecision.Valid
  }

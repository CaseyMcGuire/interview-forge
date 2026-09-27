package com.application.execution

import com.application.schema.TestDriverType

/** A supported driver contract and the language-specific instructions for implementing it. */
data class TestDriverDocumentation(
  val type: TestDriverType,
  val displayName: String,
  val documentationMarkdown: String,
)

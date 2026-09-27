package com.application.execution

import com.application.schema.TestDriverType

/** Defines the source files and commands the runner needs to execute a solution in a language. */
interface LanguageExecutionConfig {
  /** Matches the language catalog key and the entry in execution.runtimes. */
  val key: String

  /** Supported driver contracts, with authoring instructions for this language. */
  val testDriverTypes: List<TestDriverDocumentation>

  /** Builds the source-file map and commands needed to compile and run the selected driver type. */
  fun prepare(
    sourceCode: String,
    testDriverCode: String,
    testDriverType: TestDriverType = TestDriverType.MAIN_FUNCTION,
  ): PreparedProgram
}

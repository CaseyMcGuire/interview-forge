package com.application.execution

/** Language-specific preparation; the runner selects the runtime from application configuration. */
interface Language {
  /** Matches the language catalog key and the entry in execution.runtimes. */
  val key: String

  /** Describes the program without writing files, validating source, or starting processes. */
  fun prepare(sourceCode: String, testDriverCode: String): PreparedProgram
}

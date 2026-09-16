package com.application.execution

/** Defines the source files and commands the runner needs to execute a solution in a language. */
interface LanguageExecutionConfig {
  /** Matches the language catalog key and the entry in execution.runtimes. */
  val key: String

  /** Describes the program without writing files, validating source, or starting processes. */
  fun prepare(sourceCode: String, testDriverCode: String): PreparedProgram
}

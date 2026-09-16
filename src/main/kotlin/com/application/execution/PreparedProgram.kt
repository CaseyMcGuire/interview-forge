package com.application.execution

/** Files and commands relative to a single submission's workspace in the selected runtime. */
class PreparedProgram(
  /** Relative file names and their exact contents, including the private test driver. */
  val sourceFiles: Map<String, String>,
  /** Executable and arguments, without shell expansion; null when compilation is unnecessary. */
  val compileCommand: List<String>?,
  /** Executable and arguments to run after successful compilation, once per test case. */
  val runCommand: List<String>,
)

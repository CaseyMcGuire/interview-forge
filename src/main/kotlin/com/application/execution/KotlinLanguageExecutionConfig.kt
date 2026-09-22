package com.application.execution

import org.springframework.stereotype.Component

/**
 * Prepares Kotlin/JVM programs for the bundled runtime, including its Jackson libraries.
 * The test driver must define a top-level main in the default package, without @file:JvmName.
 */
@Component
class KotlinLanguageExecutionConfig : LanguageExecutionConfig {
  override val key = "kotlin"

  override fun prepare(
    sourceCode: String,
    testDriverCode: String,
  ): PreparedProgram = PreparedProgram(
    sourceFiles = mapOf(
      "Solution.kt" to sourceCode,
      "TestDriver.kt" to testDriverCode,
    ),

    compileCommand = listOf(
      "kotlinc",
      "@/opt/interview-forge/compiler.args",
      "Solution.kt",
      "TestDriver.kt",
      "-jvm-target",
      "21",
      "-include-runtime",
      "-d",
      "submission.jar",
    ),

    // The suite runner invokes the driver repeatedly in the same JVM, even if the solution defines main.
    runCommand = listOf(
      "java", "-cp", "/opt/interview-forge/lib/*:submission.jar",
      "com.application.execution.KotlinTestSuite", "TestDriverKt",
    ),
  )
}

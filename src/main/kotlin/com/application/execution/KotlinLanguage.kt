package com.application.execution

import org.springframework.stereotype.Component

/**
 * Prepares Kotlin/JVM programs for a runtime providing kotlinc and java on PATH.
 * The test driver must define a top-level main in the default package, without @file:JvmName.
 */
@Component
class KotlinLanguage : Language {
  override val key = "kotlin"

  override fun prepare(sourceCode: String, testDriverCode: String): PreparedProgram = PreparedProgram(
    sourceFiles = mapOf(
      "Solution.kt" to sourceCode,
      "TestDriver.kt" to testDriverCode,
    ),
    compileCommand = listOf(
      "kotlinc", "Solution.kt", "TestDriver.kt", "-include-runtime", "-d", "submission.jar",
    ),
    // Select the driver explicitly even if the submitted solution declares its own main.
    runCommand = listOf("java", "-cp", "submission.jar", "TestDriverKt"),
  )
}

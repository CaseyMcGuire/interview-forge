package com.application.execution

import com.application.schema.TestDriverType
import org.springframework.stereotype.Component

/**
 * Prepares Kotlin/JVM programs for the bundled runtime, including its Jackson libraries.
 * Both driver formats compile alongside the submitted source and run in one JVM per suite.
 */
@Component
class KotlinLanguageExecutionConfig : LanguageExecutionConfig {
  override val key = "kotlin"

  override val testDriverTypes = listOf(
    TestDriverDocumentation(
      type = TestDriverType.MAIN_FUNCTION,
      displayName = "Main function",
      documentationMarkdown = readDriverDocumentation("main-function.md"),
    ),
    TestDriverDocumentation(
      type = TestDriverType.JSON_TEST_DRIVER,
      displayName = "Typed JSON driver",
      documentationMarkdown = readDriverDocumentation("json-test-driver.md"),
    ),
  )

  override fun prepare(
    sourceCode: String,
    testDriverCode: String,
    testDriverType: TestDriverType,
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
      "com.application.execution.KotlinTestSuite", testDriverType.name,
    ),
  )

  private fun readDriverDocumentation(filename: String): String =
    checkNotNull(javaClass.getResource("/execution/kotlin/$filename")) {
      "Missing Kotlin test driver documentation: $filename"
    }.readText()
}

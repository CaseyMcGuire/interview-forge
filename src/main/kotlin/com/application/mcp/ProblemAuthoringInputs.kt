package com.application.mcp

import com.application.services.CreateProblemTestCase
import org.springframework.ai.mcp.annotation.McpToolParam

data class ProblemLanguageInput(
  @field:McpToolParam(description = "An enabled language key returned by list_languages.")
  val languageKey: String,
  val starterCode: String,
  @field:McpToolParam(description = "For Kotlin, a top-level main in the default package that reads one JSON value " +
    "from stdin, calls Solution, and prints one JSON value. It runs once per case in the same JVM.")
  val testDriverCode: String,
  @field:McpToolParam(description = "A correct solution implementing the same interface as starterCode, " +
    "used to generate expected outputs.")
  val referenceSolutionCode: String,
  @field:McpToolParam(description = "Per-case time limit, 1 to 60,000 milliseconds.")
  val timeLimitMs: Int,
  @field:McpToolParam(description = "Memory limit, 1 to 8,192 megabytes.")
  val memoryLimitMb: Int,
)

data class ProblemTestCaseInput(
  @field:McpToolParam(description = "Serialized JSON input, at most 20,000 characters when compactly serialized.")
  val inputJson: String,
  @field:McpToolParam(description = "Serialized JSON expectation. Use the string null for a JSON null answer.")
  val expectedOutputJson: String,
  @field:McpToolParam(required = false, description = "Optional explanation, at most 10,000 characters.")
  val explanationMarkdown: String? = null,
)

internal fun ProblemTestCaseInput.toCreateProblemTestCase() = CreateProblemTestCase(
  inputJson = inputJson,
  expectedOutputJson = expectedOutputJson,
  explanationMarkdown = explanationMarkdown,
)

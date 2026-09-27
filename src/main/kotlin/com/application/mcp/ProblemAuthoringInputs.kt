package com.application.mcp

import com.application.services.CreateProblemTestCase
import com.application.schema.TestDriverType
import org.springframework.ai.mcp.annotation.McpToolParam

data class ProblemLanguageInput(
  @field:McpToolParam(description = "An enabled language key returned by list_languages.")
  val languageKey: String,
  val starterCode: String,
  @field:McpToolParam(description = "Code compiled with the submitted source using the selected testDriverType. " +
    "See list_languages for each driver's requirements and examples.")
  val testDriverCode: String,
  @field:McpToolParam(description = "A correct solution implementing the same interface as starterCode, " +
    "used to generate expected outputs.")
  val referenceSolutionCode: String,
  @field:McpToolParam(description = "Per-case time limit, 1 to 60,000 milliseconds.")
  val timeLimitMs: Int,
  @field:McpToolParam(description = "Memory limit, 1 to 8,192 megabytes.")
  val memoryLimitMb: Int,
  @field:McpToolParam(required = false, description = "Driver contract. Defaults to MAIN_FUNCTION when omitted.")
  val testDriverType: TestDriverType = TestDriverType.MAIN_FUNCTION,
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

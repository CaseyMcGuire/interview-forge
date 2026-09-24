package com.application.mcp

import com.application.security.CurrentUserService
import com.application.schema.TestCaseVisibility
import com.application.services.ProblemInputException
import com.application.services.ProblemService
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class ProblemTestTools(
  private val problemService: ProblemService,
  private val currentUserService: CurrentUserService,
  private val results: McpToolResults,
) {
  @McpTool(
    name = "add_test_cases",
    description = "Append public examples and hidden grading cases to an existing problem in one transaction. " +
      "Provide 1 to 100 cases total, including at most 20 public examples. New public examples precede new grading cases. " +
      "Expectations are saved as supplied; use generate_expected_output first when needed. Repeating a call adds another batch.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = false, openWorldHint = false),
  )
  fun addTestCases(
    slug: String,
    @McpToolParam(required = false, description = "Hidden grading cases to append. Defaults to none.")
    testCases: List<ProblemTestCaseInput>?,
    @McpToolParam(required = false, description = "Public examples to append. Defaults to none.")
    publicExamples: List<ProblemTestCaseInput>?,
  ): CallToolResult = results.withValidationErrors {
    currentUserService.requireAdmin()
    val problem = problemService.findPublicProblemBySlug(slug)
      ?: throw ProblemInputException("slug", "Problem is unavailable")

    val added = problemService.addProblemTestCases(
      problemId = problem.id,
      publicExamples = publicExamples.orEmpty().map { it.toCreateProblemTestCase() },
      testCases = testCases.orEmpty().map { it.toCreateProblemTestCase() },
    ) ?: throw ProblemInputException("slug", "Problem is unavailable")

    results.success(AddedTestCasesResult(
      publicExamples = added.filter { it.visibility == TestCaseVisibility.EXAMPLE }.map { it.toTestCaseDetails() },
      testCases = added.filter { it.visibility == TestCaseVisibility.HIDDEN }.map { it.toTestCaseDetails() },
    ))
  }

  @McpTool(
    name = "update_test_case",
    description = "Replace a public example or grading case's input, expectation, and explanation. " +
      "The case's visibility and position stay fixed. Omitting explanationMarkdown clears the explanation.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false),
  )
  fun updateTestCase(
    @McpToolParam(description = "The decimal test-case ID returned by get_problem or add_test_cases.")
    testCaseId: String,
    inputJson: String,
    expectedOutputJson: String,
    @McpToolParam(required = false, description = "Replacement explanation; omit or pass null to clear it.")
    explanationMarkdown: String?,
  ): CallToolResult = results.withValidationErrors {
    currentUserService.requireAdmin()
    val id = testCaseId.toLongOrNull()?.takeIf { it > 0 }
      ?: throw ProblemInputException("testCaseId", "Provide a valid test-case ID")

    val updated = problemService.updateProblemTestCase(id, inputJson, expectedOutputJson, explanationMarkdown)
      ?: throw ProblemInputException("testCaseId", "Test case is unavailable")

    results.success(UpdatedTestCaseResult(updated.toTestCaseDetails()))
  }
}

data class AddedTestCasesResult(
  val publicExamples: List<TestCaseDetails>,
  val testCases: List<TestCaseDetails>,
)

data class UpdatedTestCaseResult(val testCase: TestCaseDetails)

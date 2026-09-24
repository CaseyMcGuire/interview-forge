package com.application.mcp

import com.application.ent.ProblemLanguage
import com.application.ent.ProblemSubmission
import com.application.security.CurrentUserService
import com.application.services.ProblemInputException
import com.application.services.ProblemService
import com.application.services.ProblemSubmissionService
import com.application.services.SubmitSolutionOutcome
import com.application.services.TestCaseExpectedOutputOutcome
import com.application.services.TestCaseExpectedOutputService
import entkt.runtime.query.requireLoaded
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class ProblemExecutionTools(
  private val problemService: ProblemService,
  private val expectedOutputService: TestCaseExpectedOutputService,
  private val submissionService: ProblemSubmissionService,
  private val currentUserService: CurrentUserService,
  private val results: McpToolResults,
) {
  @McpTool(
    name = "generate_expected_output",
    description = "Compile and run the stored reference solution for one JSON input and return its expected output. " +
      "Waits for execution to finish; does not save a test case or submission. " +
      "Pass the returned expectedOutputJson to add_test_cases or update_test_case.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = false, openWorldHint = false),
  )
  fun generateExpectedOutput(
    slug: String,
    languageKey: String,
    @McpToolParam(description = "Serialized JSON input, at most 20,000 characters when compactly serialized.")
    inputJson: String,
  ): CallToolResult = results.withValidationErrors {
    currentUserService.requireAdmin()
    val configuration = requireLanguageConfiguration(slug, languageKey)

    when (val outcome = expectedOutputService.generateExpectedOutput(configuration.id, inputJson)) {
      is TestCaseExpectedOutputOutcome.Success ->
        results.success(ExpectedOutputResult(outcome.expectedOutput.toString()))

      TestCaseExpectedOutputOutcome.NotFound ->
        results.failure("Problem language is unavailable", "languageKey")

      TestCaseExpectedOutputOutcome.Unavailable ->
        results.failure("Reference execution is unavailable for this problem and language")

      is TestCaseExpectedOutputOutcome.ReferenceFailed -> results.failure(outcome.message)
    }
  }

  @McpTool(
    name = "enqueue_problem_submission",
    description = "Queue code against all saved public examples and grading cases. " +
      "Omit sourceCode to verify the stored reference solution. Returns immediately with a persisted submission ID; " +
      "poll get_problem_submission until FINISHED. Each successful call creates another submission.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = false, openWorldHint = false),
  )
  fun enqueueProblemSubmission(
    slug: String,
    languageKey: String,
    @McpToolParam(required = false, description = "Solution source code; omit to use the stored reference solution.")
    sourceCode: String?,
  ): CallToolResult = results.withValidationErrors {
    currentUserService.requireAdmin()
    val configuration = requireLanguageConfiguration(slug, languageKey)
    val source = sourceCode ?: problemService.findJudgeConfiguration(configuration.id)?.referenceSolutionCode
      ?: throw ProblemInputException("sourceCode", "Configure a reference solution or supply sourceCode")

    when (val outcome = submissionService.submitSolution(configuration.id, source)) {
      is SubmitSolutionOutcome.Success ->
        results.success(ProblemSubmissionLookupResult(describeSubmission(outcome.problemSubmission)))

      SubmitSolutionOutcome.AuthenticationRequired -> results.failure("Authentication is required")
      SubmitSolutionOutcome.NotFound -> results.failure("Problem language is unavailable", "languageKey")
      SubmitSolutionOutcome.Unavailable -> results.failure("Execution is unavailable for this problem and language")
      SubmitSolutionOutcome.Busy -> results.failure("Execution capacity is full; try again later")
    }
  }

  @McpTool(
    name = "get_problem_submission",
    description = "Read a submission owned by the configured MCP account. Returns status, verdict, counts, " +
      "timing, and the first failed public example when available. Hidden failure details are not returned. " +
      "Returns problemSubmission: null for missing, malformed, or other users' submission IDs.",
    generateOutputSchema = true,
    annotations = McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
  )
  fun getProblemSubmission(
    @McpToolParam(description = "The decimal submission ID returned by enqueue_problem_submission.")
    submissionId: String,
  ): ProblemSubmissionLookupResult {
    currentUserService.requireAdmin()
    val id = submissionId.toLongOrNull()?.takeIf { it > 0 } ?: return ProblemSubmissionLookupResult(null)
    val submission = submissionService.findProblemSubmissionForCurrentUser(id)
      ?: return ProblemSubmissionLookupResult(null)

    val failedExample = submissionService.findFailedExampleForCurrentUser(id)?.let {
      FailedExampleDetails(it.inputJson.toString(), checkNotNull(it.expectedOutputJson).toString(), it.stdout.orEmpty())
    }

    return ProblemSubmissionLookupResult(describeSubmission(submission).copy(failedExample = failedExample))
  }

  private fun requireLanguageConfiguration(slug: String, languageKey: String): ProblemLanguage {
    val problem = problemService.findPublicProblemBySlug(slug)
      ?: throw ProblemInputException("slug", "Problem is unavailable")

    return problem.edges.languageConfigurations.requireLoaded().singleOrNull {
      it.edges.language.requireLoaded()?.key == languageKey
    } ?: throw ProblemInputException("languageKey", "Language is unavailable for this problem")
  }

  private fun describeSubmission(submission: ProblemSubmission) = ProblemSubmissionDetails(
    id = submission.id.toString(),
    status = submission.status,
    verdict = submission.verdict,
    totalCases = submission.totalCases,
    passedCases = submission.passedCases,
    runtimeMs = submission.runtimeMs,
    publicErrorMessage = submission.publicErrorMessage,
    createdAt = submission.createdAt.toString(),
    startedAt = submission.startedAt?.toString(),
    finishedAt = submission.finishedAt?.toString(),
  )
}

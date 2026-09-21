package com.application.mcp

import com.application.ent.Problem
import com.application.schema.ProblemDifficulty
import com.application.security.CurrentUser
import com.application.services.CreateProblem
import com.application.services.CreateProblemLanguage
import com.application.services.CreateProblemTestCase
import com.application.services.ProblemInputException
import com.application.services.ProblemJudgeConfiguration
import com.application.services.ProblemService
import com.application.services.UpdateProblem
import entkt.runtime.result.EntValidationException
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProblemAuthoringTools(
  private val problemService: ProblemService,
  private val currentUser: CurrentUser,
  private val mapper: ObjectMapper,
) {
  @McpTool(
    name = "create_problem",
    description = "Create and publish a problem with starter code, a test driver, a reference solution, " +
      "public examples, and hidden test cases in one transaction. All writes roll back if any input is invalid. " +
      "Search for an existing problem first; an existing slug is rejected. Code is saved without executing it.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = false, openWorldHint = false),
  )
  fun createProblem(
    @McpToolParam(description = "Stable URL slug: lowercase letters, numbers, and single hyphens; at most 100 characters.")
    slug: String,
    @McpToolParam(description = "Problem title, 1 to 200 characters.")
    title: String,
    @McpToolParam(description = "Requirements and constraints in Markdown, at most 100,000 characters.")
    statementMarkdown: String,
    difficulty: ProblemDifficulty,
    @McpToolParam(description = "Between 1 and 20 distinct enabled languages, each with complete execution code and limits.")
    languageConfigurations: List<ProblemLanguageInput>,
    @McpToolParam(description = "Between 1 and 20 public examples shown on the problem page.")
    publicExamples: List<ProblemTestCaseInput>,
    @McpToolParam(required = false, description = "Up to 100 hidden grading cases, executed after the public examples. Defaults to none.")
    testCases: List<ProblemTestCaseInput>?,
  ): CallToolResult = reportProblemWrite {
    problemService.createProblem(CreateProblem(
      slug = slug,
      title = title,
      statementMarkdown = statementMarkdown,
      difficulty = difficulty,
      languageConfigurations = languageConfigurations.map(::toLanguageConfiguration),
      examples = publicExamples.map(::toTestCase),
      testCases = testCases.orEmpty().map(::toTestCase),
    ))
  }

  @McpTool(
    name = "update_problem",
    description = "Update a problem's title, statement, or difficulty by slug. " +
      "Omitted or null fields stay unchanged. The slug, language configuration, and tests stay unchanged.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false),
  )
  fun updateProblem(
    slug: String,
    @McpToolParam(required = false, description = "Replacement title, 1 to 200 characters.")
    title: String?,
    @McpToolParam(required = false, description = "Replacement statement in Markdown, at most 100,000 characters.")
    statementMarkdown: String?,
    @McpToolParam(required = false, description = "Replacement difficulty.")
    difficulty: ProblemDifficulty?,
  ): CallToolResult = reportProblemWrite {
    val problem = requireProblem(slug)
    problemService.updateProblem(UpdateProblem(problem.id, title, statementMarkdown, difficulty))
      ?: throw ProblemInputException("slug", "Problem is unavailable")
  }

  @McpTool(
    name = "configure_problem_language",
    description = "Add a language to a problem, or replace its starter code, test driver, reference solution, " +
      "and execution limits together. The problem must exist and the language must be enabled. " +
      "Code is saved without executing it.",
    annotations = McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false),
  )
  fun configureProblemLanguage(
    slug: String,
    configuration: ProblemLanguageInput,
  ): CallToolResult = reportProblemWrite {
    val problem = requireProblem(slug)
    problemService.configureProblemLanguage(
      problemId = problem.id,
      languageKey = configuration.languageKey,
      starterCode = configuration.starterCode,
      testDriverCode = configuration.testDriverCode,
      referenceSolutionCode = configuration.referenceSolutionCode,
      timeLimitMs = configuration.timeLimitMs,
      memoryLimitMb = configuration.memoryLimitMb,
    ) ?: throw ProblemInputException("slug", "Problem is unavailable")
  }

  private fun requireProblem(slug: String): Problem = problemService.findPublicProblemBySlug(slug)
    ?: throw ProblemInputException("slug", "Problem is unavailable")

  /** Preserve field errors as structured content while also marking the MCP call as failed. */
  private fun reportProblemWrite(write: () -> Problem): CallToolResult {
    currentUser.requireAdmin()

    val result = try {
      val problem = write()
      ProblemWriteResult(problem = ProblemSummary(problem.slug, problem.title, problem.difficulty))
    } catch (exception: EntValidationException) {
      ProblemWriteResult(errors = exception.violations.map { ProblemWriteError(it.field, it.message) })
    } catch (exception: ProblemInputException) {
      ProblemWriteResult(errors = listOf(ProblemWriteError(exception.field, exception.message)))
    } catch (exception: IllegalArgumentException) {
      ProblemWriteResult(errors = listOf(ProblemWriteError(null, exception.message ?: "Invalid problem input")))
    }

    return CallToolResult.builder()
      .structuredContent(result)
      .addTextContent(mapper.writeValueAsString(result))
      .isError(result.errors.isNotEmpty())
      .build()
  }

  private fun toLanguageConfiguration(input: ProblemLanguageInput) = CreateProblemLanguage(
    languageKey = input.languageKey,
    starterCode = input.starterCode,
    judgeConfiguration = ProblemJudgeConfiguration(
      testDriverCode = input.testDriverCode,
      referenceSolutionCode = input.referenceSolutionCode,
      timeLimitMs = input.timeLimitMs,
      memoryLimitMb = input.memoryLimitMb,
    ),
  )

  private fun toTestCase(input: ProblemTestCaseInput) = CreateProblemTestCase(
    inputJson = input.inputJson,
    expectedOutputJson = input.expectedOutputJson,
    explanationMarkdown = input.explanationMarkdown,
  )
}

data class ProblemWriteResult(
  val problem: ProblemSummary? = null,
  val errors: List<ProblemWriteError> = emptyList(),
)

data class ProblemWriteError(val field: String?, val message: String)

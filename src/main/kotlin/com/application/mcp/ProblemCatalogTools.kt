package com.application.mcp

import com.application.ent.ProblemLanguage
import com.application.schema.ProblemDifficulty
import com.application.security.CurrentUserService
import com.application.services.ProblemCursor
import com.application.services.ProblemService
import entkt.runtime.query.requireLoaded
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class ProblemCatalogTools(
  private val problemService: ProblemService,
  private val currentUserService: CurrentUserService,
) {
  @McpTool(
    name = "list_languages",
    description = "List enabled programming languages and their stable keys for problem authoring.",
    generateOutputSchema = true,
    annotations = McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
  )
  fun listLanguages(): LanguageCatalogResult {
    currentUserService.requireAdmin()

    val languages = problemService.findEnabledLanguages().map { LanguageSummary(it.key, it.displayName) }
    return LanguageCatalogResult(languages)
  }

  @McpTool(
    name = "search_problems",
    description = "Find published, unarchived problems by title and difficulty. " +
      "Results are ordered by title and slug; use nextCursor with the same filters to continue.",
    generateOutputSchema = true,
    annotations = McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
  )
  fun searchProblems(
    @McpToolParam(required = false, description = "Case-sensitive title substring; omit to list all problems.")
    search: String?,
    @McpToolParam(required = false, description = "Optional difficulty filter.")
    difficulty: ProblemDifficulty?,
    @McpToolParam(required = false, description = "Maximum results, from 1 to 100. Defaults to 20.")
    first: Int?,
    @McpToolParam(required = false, description = "The nextCursor returned by the previous search.")
    after: String?,
  ): ProblemSearchResult {
    currentUserService.requireAdmin()

    val pageSize = first ?: 20
    require(pageSize in 1..100) { "first must be between 1 and 100" }
    val cursor = after?.let { decodeCursor(it) }
    val page = problemService.findPublicProblems(pageSize, cursor, search, difficulty)

    val problems = page.problems.map { ProblemSummary(it.slug, it.title, it.difficulty) }
    val nextCursor = if (page.hasNextPage) {
      val last = page.problems.last()
      ProblemCursor(last.title, last.slug).encode()
    } else {
      null
    }

    return ProblemSearchResult(problems, nextCursor)
  }

  @McpTool(
    name = "get_problem",
    description = "Read a published, unarchived problem for authoring, including starter code, " +
      "private judge settings, publicExamples, and hidden testCases. Returns problem: null when unavailable.",
    generateOutputSchema = true,
    annotations = McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false),
  )
  fun getProblem(
    @McpToolParam(description = "The problem's stable URL slug, such as three-card-poker.")
    slug: String,
  ): ProblemLookupResult {
    currentUserService.requireAdmin()
    require(slug.isNotBlank()) { "slug must not be blank" }

    val problem = problemService.findPublicProblemBySlug(slug) ?: return ProblemLookupResult(null)
    val configurations = problem.edges.languageConfigurations.requireLoaded()
      .map(::readLanguageConfiguration)
      .sortedBy { it.languageKey }
    val examples = problem.edges.testCases.requireLoaded().map { it.toTestCaseDetails() }
    val testCases = problemService.findProblemTestCases(problem.id)
      ?: return ProblemLookupResult(null)

    return ProblemLookupResult(ProblemDetails(
      slug = problem.slug,
      title = problem.title,
      statementMarkdown = problem.statementMarkdown,
      difficulty = problem.difficulty,
      languageConfigurations = configurations,
      publicExamples = examples,
      testCases = testCases.map { it.toTestCaseDetails() },
    ))
  }

  private fun readLanguageConfiguration(configuration: ProblemLanguage): ProblemLanguageDetails {
    val language = checkNotNull(configuration.edges.language.requireLoaded())
    val judge = problemService.findJudgeConfiguration(configuration.id)

    return ProblemLanguageDetails(
      languageKey = language.key,
      starterCode = configuration.starterCode,
      judgeConfiguration = judge?.let {
        JudgeConfigurationDetails(
          testDriverCode = it.testDriverCode,
          referenceSolutionCode = it.referenceSolutionCode,
          checkerSource = it.checkerSource,
          timeLimitMs = it.timeLimitMs,
          memoryLimitMb = it.memoryLimitMb,
        )
      },
    )
  }

  private fun decodeCursor(value: String): ProblemCursor = try {
    ProblemCursor.decode(value)
  } catch (_: IllegalArgumentException) {
    throw IllegalArgumentException("after must be a cursor returned by search_problems")
  }
}

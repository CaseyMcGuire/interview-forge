package com.application.graphql

import com.application.services.ProblemService
import com.application.graphql.types.Language
import com.application.graphql.types.Problem
import com.application.graphql.types.ProblemDifficulty
import com.application.graphql.types.ProblemExample
import com.application.graphql.types.ProblemLanguage
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import entkt.runtime.query.requireLoaded

@DgsComponent
class ProblemDataFetcher(
  private val problemService: ProblemService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsQuery
  fun problem(@InputArgument slug: String): Problem? {
    val problem = problemService.findPublicProblemBySlug(slug) ?: return null

    return Problem(
      id = globalIdUtil.toGlobalId(Problem::class, problem.id),
      slug = problem.slug,
      title = problem.title,
      statementMarkdown = problem.statementMarkdown,
      difficulty = ProblemDifficulty.valueOf(problem.difficulty.name),
      languageConfigurations = problem.edges.languageConfigurations.requireLoaded().mapNotNull { configuration ->
        val language = configuration.edges.language.requireLoaded() ?: return@mapNotNull null
        ProblemLanguage(
          id = globalIdUtil.toGlobalId(ProblemLanguage::class, configuration.id),
          language = Language(
            id = globalIdUtil.toGlobalId(Language::class, language.id),
            key = language.key,
            displayName = language.displayName,
          ),
          starterCode = configuration.starterCode,
          solutionFilename = configuration.solutionFilename,
        )
      }.sortedBy { it.language.key },
      examples = problem.edges.testCases.requireLoaded().map { example ->
        ProblemExample(
          id = globalIdUtil.toGlobalId(ProblemExample::class, example.id),
          position = example.position,
          inputJson = example.inputJson.toString(),
          expectedOutputJson = example.expectedOutputJson.toString(),
          explanationMarkdown = example.explanationMarkdown,
        )
      },
    )
  }
}

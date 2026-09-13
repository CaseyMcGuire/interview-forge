package com.application.graphql

import com.application.services.ProblemService
import com.application.services.ProblemCursor
import com.application.services.CreateProblem
import com.application.services.CreateProblemExample
import com.application.services.CreateProblemLanguage
import com.application.services.UpdateProblem
import com.application.services.UpdateProblemExample
import com.application.services.UpdateProblemLanguage
import com.application.services.ProblemContentChangedException
import com.application.graphql.types.CreateProblemInput
import com.application.graphql.types.UpdateProblemInput
import com.application.graphql.types.UpdateProblemResult
import com.application.graphql.types.UpdateProblemSuccess
import com.application.graphql.types.UpdateProblemValidationFailure
import com.application.graphql.types.UpdateProblemNotFound
import com.application.graphql.types.UpdateProblemForbidden
import com.application.graphql.types.UpdateProblemContentChanged
import com.application.graphql.types.Language
import com.application.graphql.types.Problem
import com.application.graphql.types.ProblemDifficulty
import com.application.graphql.types.ProblemExample
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemConnection
import com.application.graphql.types.ProblemEdge
import com.application.graphql.types.ProblemFilterInput
import com.application.graphql.types.PageInfo
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsBadRequestException
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntValidationException
import entkt.runtime.result.EntMutationPrivacyDeniedException
import org.springframework.security.access.AccessDeniedException
import com.application.ent.Problem as ProblemEntity
import com.application.schema.ProblemDifficulty as SchemaProblemDifficulty

@DgsComponent
class ProblemDataFetcher(
  private val problemService: ProblemService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsQuery
  fun languages(): List<Language> = problemService.findEnabledLanguages().map { language ->
    Language(
      id = globalIdUtil.toGlobalId(Language::class, language.id),
      key = language.key,
      displayName = language.displayName,
    )
  }

  @DgsMutation
  fun createProblem(@InputArgument input: CreateProblemInput): Problem {
    val problem = try {
      problemService.createProblem(CreateProblem(
        slug = input.slug,
        title = input.title,
        statementMarkdown = input.statementMarkdown,
        difficulty = SchemaProblemDifficulty.valueOf(input.difficulty.name),
        languageConfigurations = input.languageConfigurations.map {
          CreateProblemLanguage(it.languageKey, it.starterCode, it.solutionFilename)
        },
        examples = input.examples.map {
          CreateProblemExample(it.inputJson, it.expectedOutputJson, it.explanationMarkdown)
        },
      ))
    } catch (exception: EntValidationException) {
      throw DgsBadRequestException(exception.violations.joinToString("; ") { it.message })
    } catch (exception: IllegalArgumentException) {
      throw DgsBadRequestException(exception.message ?: "Invalid problem")
    }
    return toGraphqlProblem(problem)
  }

  @DgsMutation
  fun updateProblem(@InputArgument slug: String, @InputArgument input: UpdateProblemInput): UpdateProblemResult {
    val problem = try {
      problemService.updateProblem(slug, UpdateProblem(
        title = input.title,
        statementMarkdown = input.statementMarkdown,
        difficulty = SchemaProblemDifficulty.valueOf(input.difficulty.name),
        languageConfigurations = input.languageConfigurations.map {
          UpdateProblemLanguage(
            id = requireNotNull(globalIdUtil.fromGlobalIdOrNull(it.id, ProblemLanguage::class)) {
              "Invalid language configuration ID"
            },
            starterCode = it.starterCode,
            solutionFilename = it.solutionFilename,
          )
        },
        examples = input.examples.map {
          UpdateProblemExample(
            id = requireNotNull(globalIdUtil.fromGlobalIdOrNull(it.id, ProblemExample::class)) {
              "Invalid example ID"
            },
            inputJson = it.inputJson,
            expectedOutputJson = it.expectedOutputJson,
            explanationMarkdown = it.explanationMarkdown,
          )
        },
      ))
    } catch (exception: ProblemContentChangedException) {
      return UpdateProblemContentChanged(message = requireNotNull(exception.message))
    } catch (_: AccessDeniedException) {
      return UpdateProblemForbidden(message = "Administrator access is required")
    } catch (_: EntMutationPrivacyDeniedException) {
      return UpdateProblemForbidden(message = "You no longer have permission to update this problem")
    } catch (exception: EntValidationException) {
      return UpdateProblemValidationFailure(message = exception.violations.joinToString("; ") { it.message })
    } catch (exception: IllegalArgumentException) {
      return UpdateProblemValidationFailure(message = exception.message ?: "Invalid problem")
    }

    return if (problem == null) {
      UpdateProblemNotFound(message = "This problem is no longer available. Your edits are still here.")
    } else {
      UpdateProblemSuccess(problem = toGraphqlProblem(problem))
    }
  }

  @DgsQuery
  fun problems(
    @InputArgument first: Int?,
    @InputArgument after: String?,
    @InputArgument filters: ProblemFilterInput?,
  ): ProblemConnection {
    val pageSize = first ?: 20
    if (pageSize !in 0..100) throw DgsBadRequestException("first must be between 0 and 100")
    val cursor = after?.let {
      try {
        ProblemCursor.decode(it)
      } catch (_: IllegalArgumentException) {
        throw DgsBadRequestException("Invalid problem cursor")
      }
    }
    val page = problemService.findPublicProblems(
      first = pageSize,
      after = cursor,
      search = filters?.search,
      difficulty = filters?.difficulty?.let { SchemaProblemDifficulty.valueOf(it.name) },
    )
    val edges = page.problems.map { problem ->
      ProblemEdge(
        cursor = ProblemCursor(problem.title, problem.slug).encode(),
        node = toGraphqlProblem(problem),
      )
    }
    return ProblemConnection(
      edges = edges,
      pageInfo = PageInfo(
        hasNextPage = page.hasNextPage,
        hasPreviousPage = page.hasPreviousPage,
        startCursor = edges.firstOrNull()?.cursor,
        endCursor = edges.lastOrNull()?.cursor,
      ),
    )
  }

  @DgsQuery
  fun problem(@InputArgument slug: String): Problem? {
    val problem = problemService.findPublicProblemBySlug(slug) ?: return null

    return toGraphqlProblem(problem)
  }

  private fun toGraphqlProblem(problem: ProblemEntity): Problem =
    Problem(
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

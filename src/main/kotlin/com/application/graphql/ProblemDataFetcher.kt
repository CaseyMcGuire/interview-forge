package com.application.graphql

import com.application.services.ProblemService
import com.application.services.ProblemCursor
import com.application.services.CreateProblem
import com.application.services.CreateProblemHiddenTestCase
import com.application.services.CreateProblemExample
import com.application.services.CreateProblemLanguage
import com.application.services.FieldUpdate
import com.application.services.ProblemInputException
import com.application.services.UpdateProblem
import com.application.services.UpdateProblemExample
import com.application.services.UpdateProblemLanguage
import com.application.graphql.types.CreateProblemInput
import com.application.graphql.types.CreateProblemHiddenTestCaseInput
import com.application.graphql.types.CreateProblemHiddenTestCaseResult
import com.application.graphql.types.CreateProblemHiddenTestCaseSuccess
import com.application.graphql.types.Language
import com.application.graphql.types.Problem
import com.application.graphql.types.ProblemDifficulty
import com.application.graphql.types.ProblemExample
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemConnection
import com.application.graphql.types.ProblemEdge
import com.application.graphql.types.ProblemFilterInput
import com.application.graphql.types.PageInfo
import com.application.graphql.types.DeleteProblemExampleInput
import com.application.graphql.types.DeleteProblemExampleResult
import com.application.graphql.types.DeleteProblemExampleSuccess
import com.application.graphql.types.FieldError
import com.application.graphql.types.ProblemForbidden
import com.application.graphql.types.ProblemNotFound
import com.application.graphql.types.ProblemValidationFailure
import com.application.graphql.types.UpdateProblemInput
import com.application.graphql.types.UpdateProblemResult
import com.application.graphql.types.UpdateProblemSuccess
import com.application.graphql.types.UpdateProblemExampleInput
import com.application.graphql.types.UpdateProblemExampleResult
import com.application.graphql.types.UpdateProblemExampleSuccess
import com.application.graphql.types.UpdateProblemLanguageInput
import com.application.graphql.types.UpdateProblemLanguageResult
import com.application.graphql.types.UpdateProblemLanguageSuccess
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsBadRequestException
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntTargetAbsentException
import entkt.runtime.result.EntValidationException
import graphql.schema.DataFetchingEnvironment
import org.springframework.security.access.AccessDeniedException
import kotlin.reflect.KClass
import com.application.ent.Problem as ProblemEntity
import com.application.ent.ProblemLanguage as ProblemLanguageEntity
import com.application.ent.TestCase as TestCaseEntity
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
          CreateProblemLanguage(it.languageKey, it.starterCode)
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

  @DgsMutation
  fun createProblemHiddenTestCase(
    @InputArgument input: CreateProblemHiddenTestCaseInput,
  ): CreateProblemHiddenTestCaseResult = try {
    val problem = problemService.createProblemHiddenTestCase(CreateProblemHiddenTestCase(
      problemId = problemContentId(input.problemId, Problem::class, "problemId"),
      inputJson = input.inputJson,
      expectedOutputJson = input.expectedOutputJson,
      explanationMarkdown = input.explanationMarkdown,
    ))

    if (problem == null) {
      contentNotFound()
    } else {
      CreateProblemHiddenTestCaseSuccess(toGraphqlProblem(problem))
    }
  } catch (_: AccessDeniedException) {
    contentForbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    contentNotFound()
  } catch (_: EntTargetAbsentException) {
    contentNotFound()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: ProblemInputException) {
    validationFailure(exception)
  }

  @DgsQuery
  fun problem(@InputArgument slug: String): Problem? {
    val problem = problemService.findPublicProblemBySlug(slug) ?: return null

    return toGraphqlProblem(problem)
  }

  @DgsMutation
  fun updateProblem(
    @InputArgument input: UpdateProblemInput,
  ): UpdateProblemResult = try {
    val problem = problemService.updateProblem(UpdateProblem(
      id = problemContentId(input.id, Problem::class),
      title = input.title,
      statementMarkdown = input.statementMarkdown,
      difficulty = input.difficulty?.let { SchemaProblemDifficulty.valueOf(it.name) },
    ))

    if (problem == null) {
      contentNotFound()
    } else {
      UpdateProblemSuccess(toGraphqlProblem(problem))
    }
  } catch (_: AccessDeniedException) {
    contentForbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    contentNotFound()
  } catch (_: EntTargetAbsentException) {
    contentNotFound()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: ProblemInputException) {
    validationFailure(exception)
  }

  @DgsMutation
  fun updateProblemLanguage(
    @InputArgument input: UpdateProblemLanguageInput,
  ): UpdateProblemLanguageResult = try {
    val configuration = problemService.updateProblemLanguage(UpdateProblemLanguage(
      id = problemContentId(input.id, ProblemLanguage::class),
      starterCode = input.starterCode,
    ))

    if (configuration == null) {
      contentNotFound()
    } else {
      UpdateProblemLanguageSuccess(toGraphqlProblemLanguage(configuration))
    }
  } catch (_: AccessDeniedException) {
    contentForbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    contentNotFound()
  } catch (_: EntTargetAbsentException) {
    contentNotFound()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: ProblemInputException) {
    validationFailure(exception)
  }

  @DgsMutation
  fun updateProblemExample(
    @InputArgument input: UpdateProblemExampleInput,
    environment: DataFetchingEnvironment,
  ): UpdateProblemExampleResult = try {
    // Explanations can be cleared, so only this field needs to distinguish null from omission.
    val inputFields = environment.getArgument<Map<String, Any?>>("input").orEmpty()
    val explanation = if (inputFields.containsKey("explanationMarkdown")) {
      FieldUpdate.Set(input.explanationMarkdown)
    } else {
      FieldUpdate.Unchanged
    }

    val example = problemService.updateProblemExample(UpdateProblemExample(
      id = problemContentId(input.id, ProblemExample::class),
      inputJson = input.inputJson,
      expectedOutputJson = input.expectedOutputJson,
      explanationMarkdown = explanation,
    ))

    if (example == null) {
      contentNotFound()
    } else {
      UpdateProblemExampleSuccess(toGraphqlProblemExample(example))
    }
  } catch (_: AccessDeniedException) {
    contentForbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    contentNotFound()
  } catch (_: EntTargetAbsentException) {
    contentNotFound()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: ProblemInputException) {
    validationFailure(exception)
  }

  @DgsMutation
  fun deleteProblemExample(
    @InputArgument input: DeleteProblemExampleInput,
  ): DeleteProblemExampleResult = try {
    val problem = problemService.deleteProblemExample(problemContentId(input.id, ProblemExample::class))

    if (problem == null) {
      contentNotFound()
    } else {
      DeleteProblemExampleSuccess(deletedExampleId = input.id, problem = toGraphqlProblem(problem))
    }
  } catch (_: AccessDeniedException) {
    contentForbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    contentNotFound()
  } catch (_: EntTargetAbsentException) {
    contentNotFound()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: ProblemInputException) {
    validationFailure(exception)
  }

  private fun problemContentId(value: String, type: KClass<*>, field: String = "id"): Long =
    globalIdUtil.fromGlobalIdOrNull(value, type)
      ?: throw ProblemInputException(field, "Provide a valid ${type.simpleName} ID")

  private fun contentNotFound() =
    ProblemNotFound("The requested content does not exist or is unavailable")

  private fun contentForbidden() = ProblemForbidden("Administrator access is required")

  private fun validationFailure(exception: EntValidationException) = ProblemValidationFailure(
    message = "The problem content is invalid",
    fieldErrors = exception.violations.map { FieldError(field = it.field.orEmpty(), message = it.message) },
  )

  private fun validationFailure(exception: ProblemInputException) = ProblemValidationFailure(
    message = "The problem content is invalid",
    fieldErrors = listOf(FieldError(field = exception.field, message = exception.message)),
  )

  private fun toGraphqlProblem(problem: ProblemEntity): Problem =
    Problem(
      id = globalIdUtil.toGlobalId(Problem::class, problem.id),
      slug = problem.slug,
      title = problem.title,
      statementMarkdown = problem.statementMarkdown,
      difficulty = ProblemDifficulty.valueOf(problem.difficulty.name),
      languageConfigurations = problem.edges.languageConfigurations.requireLoaded()
        .map(::toGraphqlProblemLanguage)
        .sortedBy { it.language.key },
      examples = problem.edges.testCases.requireLoaded().map(::toGraphqlProblemExample),
    )

  private fun toGraphqlProblemLanguage(configuration: ProblemLanguageEntity): ProblemLanguage {
    val language = checkNotNull(configuration.edges.language.requireLoaded())

    return ProblemLanguage(
      id = globalIdUtil.toGlobalId(ProblemLanguage::class, configuration.id),
      language = Language(
        id = globalIdUtil.toGlobalId(Language::class, language.id),
        key = language.key,
        displayName = language.displayName,
      ),
      starterCode = configuration.starterCode,
    )
  }

  private fun toGraphqlProblemExample(example: TestCaseEntity): ProblemExample = ProblemExample(
    id = globalIdUtil.toGlobalId(ProblemExample::class, example.id),
    position = example.position,
    inputJson = example.inputJson.toString(),
    expectedOutputJson = example.expectedOutputJson.toString(),
    explanationMarkdown = example.explanationMarkdown,
  )
}

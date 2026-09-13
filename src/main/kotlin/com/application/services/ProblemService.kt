package com.application.services

import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.ProblemQuery
import com.application.ent.ProblemQueryScope
import com.application.ent.TestCase
import com.application.schema.TestCaseVisibility
import com.application.schema.ProblemDifficulty
import com.application.security.CurrentUser
import entkt.runtime.result.EntConstraintViolationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import entkt.query.isNull
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import entkt.runtime.query.requireLoaded
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ProblemService(
  private val entClient: EntClient,
  private val currentUser: CurrentUser,
) {
  // This catalog view uses the same public visibility for signed-in and anonymous visitors.
  private val publicContext = ViewerContext(Viewer.Anonymous)

  fun findEnabledLanguages(): List<Language> = entClient.languages.query {
    where(Language.enabled eq true)
    orderBy(Language.key.asc())
  }.all(publicContext).getOrThrow()

  fun createProblem(input: CreateProblem): Problem {
    val author = currentUser.requireAdmin()
    validateRequest(input)
    val context = ViewerContext(Viewer.User(author.id))

    return try {
      entClient.withTransaction { tx ->
        val languageKeys = input.languageConfigurations.map { it.languageKey }
        val languages = loadEnabledLanguages(tx, languageKeys, context)

        val problem = tx.problems.create {
          slug = input.slug.trim()
          title = input.title.trim()
          statementMarkdown = input.statementMarkdown
          difficulty = input.difficulty
          createdByUserId = author.id
          publishedAt = Instant.now()
        }.saveAndLoad(context).getOrThrow()

        input.languageConfigurations.forEach { configuration ->
          tx.problemLanguages.create {
            problemId = problem.id
            languageId = languages.getValue(configuration.languageKey).id
            starterCode = configuration.starterCode
            solutionFilename = configuration.solutionFilename
          }.save(context).getOrThrow()
        }

        input.examples.forEachIndexed { index, example ->
          tx.testCases.create {
            problemId = problem.id
            position = index
            visibility = TestCaseVisibility.EXAMPLE
            inputJson = parseExampleJson(example.inputJson, "Example ${index + 1} input")
            expectedOutputJson = parseExampleJson(example.expectedOutputJson, "Example ${index + 1} expected output")
            explanationMarkdown = example.explanationMarkdown?.takeIf { it.isNotBlank() }
          }.save(context).getOrThrow()
        }

        tx.problems.query {
          where(Problem.id eq problem.id)
          loadPublicContent()
        }.firstOrNull(context).getOrThrow() ?: error("Created problem could not be loaded")
      }.getOrThrow()
    } catch (exception: EntConstraintViolationException) {
      if (exception.driverCode == "23505" && exception.constraint == "idx_problems_slug_unique") {
        throw IllegalArgumentException("A problem with this slug already exists")
      }
      throw exception
    }
  }

  private fun validateRequest(input: CreateProblem) {
    require(input.languageConfigurations.size in 1..20) { "Provide between 1 and 20 language configurations" }

    val languageKeys = input.languageConfigurations.map { it.languageKey }
    require(languageKeys.distinct().size == languageKeys.size) { "Provide only one configuration per language" }

    require(input.examples.size in 1..20) { "Provide between 1 and 20 examples" }
  }

  fun updateProblem(slug: String, input: UpdateProblem): Problem? {
    val author = currentUser.requireAdmin()
    val context = ViewerContext(Viewer.User(author.id))
    // Parse request-only data before opening the transaction or making any writes.
    val exampleInputs = input.examples.mapIndexed { index, example ->
      parseExampleJson(example.inputJson, "Example ${index + 1} input")
    }
    val exampleOutputs = input.examples.mapIndexed { index, example ->
      parseExampleJson(example.expectedOutputJson, "Example ${index + 1} expected output")
    }

    return entClient.withTransaction { tx ->
      val problem = tx.problems.query {
        where(Problem.slug eq slug)
        loadPublicContent()
      }.firstOrNull(context).visibleOrNull().getOrThrow() ?: return@withTransaction null

      // These checks depend on stored relationships, so keep them with the writes.
      validateEditRequest(input, problem)

      tx.problems.update(problem.id) {
        title = input.title.trim()
        statementMarkdown = input.statementMarkdown
        difficulty = input.difficulty
      }.save(context).getOrThrow()

      input.languageConfigurations.forEach { configuration ->
        tx.problemLanguages.update(configuration.id) {
          starterCode = configuration.starterCode
          solutionFilename = configuration.solutionFilename
        }.save(context).getOrThrow()
      }

      input.examples.forEachIndexed { index, example ->
        tx.testCases.update(example.id) {
          inputJson = exampleInputs[index]
          expectedOutputJson = exampleOutputs[index]
          explanationMarkdown = example.explanationMarkdown?.takeIf { it.isNotBlank() }
        }.save(context).getOrThrow()
      }

      tx.problems.query {
        where(Problem.id eq problem.id)
        loadPublicContent()
      }.firstOrNull(context).getOrThrow() ?: error("Updated problem could not be loaded")
    }.getOrThrow()
  }

  private fun validateEditRequest(input: UpdateProblem, problem: Problem) {
    val configurationIds = input.languageConfigurations.map { it.id }
    val existingConfigurationIds = problem.edges.languageConfigurations.requireLoaded().map { it.id }
    if (
      configurationIds.size != existingConfigurationIds.size ||
        configurationIds.toSet() != existingConfigurationIds.toSet()
    ) {
      throw ProblemContentChangedException(
        "Language configurations have changed or do not belong to this problem. Reload the edit page."
      )
    }

    val exampleIds = input.examples.map { it.id }
    val existingExampleIds = problem.edges.testCases.requireLoaded().map { it.id }
    if (exampleIds.size != existingExampleIds.size || exampleIds.toSet() != existingExampleIds.toSet()) {
      throw ProblemContentChangedException(
        "Examples have changed or do not belong to this problem. Reload the edit page."
      )
    }
  }

  private fun loadEnabledLanguages(
    tx: EntTransactionClient,
    languageKeys: List<String>,
    context: ViewerContext,
  ): Map<String, Language> {
    val languages = tx.languages.query {
      where(Language.key `in` languageKeys)
      where(Language.enabled eq true)
    }.all(context).getOrThrow().associateBy { it.key }

    require(languages.size == languageKeys.size) {
      "Every configuration must use an enabled language from the catalog"
    }

    return languages
  }

  private fun parseExampleJson(value: String, label: String): JsonElement {
    require(value.length <= 20_000) { "$label must be at most 20,000 characters" }
    return try {
      Json.parseToJsonElement(value)
    } catch (_: IllegalArgumentException) {
      throw IllegalArgumentException("$label must be valid JSON")
    }
  }

  fun findPublicProblems(
    first: Int,
    after: ProblemCursor? = null,
    search: String? = null,
    difficulty: ProblemDifficulty? = null,
  ): ProblemPage {
    require(first in 0..100) { "first must be between 0 and 100" }
    var query = publicProblemsQuery(search, difficulty)
    if (after != null) {
      query = query.where(
        (Problem.title gt after.title) or
          ((Problem.title eq after.title) and (Problem.slug gt after.slug))
      )
    }
    val selected = query
      .orderBy(Problem.title.asc())
      .orderBy(Problem.slug.asc())
      .limit(first + 1)
      .all(publicContext)
      .getOrThrow()
    val hasPreviousPage = after != null && publicProblemsQuery(search, difficulty)
      .where(
        (Problem.title lt after.title) or
          ((Problem.title eq after.title) and (Problem.slug lte after.slug))
      )
      .firstOrNull(publicContext)
      .getOrThrow() != null

    return ProblemPage(
      problems = selected.take(first),
      hasNextPage = selected.size > first,
      hasPreviousPage = hasPreviousPage,
    )
  }

  private fun publicProblemsQuery(search: String?, difficulty: ProblemDifficulty?): ProblemQuery =
    entClient.problems.query {
      where(Problem.publishedAt lte Instant.now())
      where(Problem.archivedAt.isNull())
      search?.trim()?.takeIf { it.isNotEmpty() }?.let { where(Problem.title contains it) }
      difficulty?.let { where(Problem.difficulty eq it) }
      loadPublicContent()
    }

  fun findPublicProblemBySlug(slug: String): Problem? =
    entClient.problems.query {
      where(Problem.slug eq slug)
      loadPublicContent()
    }.firstOrNull(publicContext).visibleOrNull().getOrThrow()

  private fun ProblemQueryScope.loadPublicContent() {
    loadLanguageConfigurations {
      where(ProblemLanguage.language.has { where(Language.enabled eq true) })
      loadLanguage()
    }
    loadTestCases {
      where(TestCase.visibility eq TestCaseVisibility.EXAMPLE)
      orderBy(TestCase.position.asc())
    }
  }
}

data class ProblemPage(
  val problems: List<Problem>,
  val hasNextPage: Boolean,
  val hasPreviousPage: Boolean,
)

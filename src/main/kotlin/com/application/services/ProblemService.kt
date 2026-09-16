package com.application.services

import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.JudgeConfiguration
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

  fun updateProblem(input: UpdateProblem): Problem? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))

    return entClient.withTransaction { tx ->
      tx.problems.update(input.id) {
        input.title?.let { title = it.trim() }
        input.statementMarkdown?.let { statementMarkdown = it }
        input.difficulty?.let { difficulty = it }
      }.save(context).getOrThrow()

      tx.loadProblemContent(input.id, context)
    }.getOrThrow()
  }

  fun createProblemHiddenTestCase(input: CreateProblemHiddenTestCase): Problem? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))
    val parsedInput = parseTestCaseJson(input.inputJson, "inputJson")
    val parsedOutput = parseTestCaseJson(input.expectedOutputJson, "expectedOutputJson")

    return entClient.withTransaction { tx ->
      // Lock the parent even when it has no cases, so concurrent additions cannot reuse a position.
      tx.problems.query {
        where(Problem.id eq input.problemId)
      }.forUpdate().firstOrNull(context).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      val lastPosition = tx.testCases.indexes.problemId(input.problemId).query {
        orderBy(TestCase.position.desc())
      }.firstOrNull(context).getOrThrow()?.position

      if (lastPosition == Int.MAX_VALUE) {
        throw ProblemInputException("problemId", "This problem cannot accept more test cases")
      }

      tx.testCases.create {
        problemId = input.problemId
        position = (lastPosition ?: -1) + 1
        visibility = TestCaseVisibility.HIDDEN
        inputJson = parsedInput
        expectedOutputJson = parsedOutput
        explanationMarkdown = input.explanationMarkdown
      }.save(context).getOrThrow()

      tx.loadProblemContent(input.problemId, context)
    }.getOrThrow()
  }

  fun updateProblemLanguage(input: UpdateProblemLanguage): ProblemLanguage? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))

    return entClient.withTransaction { tx ->
      tx.problemLanguages.findById(context, input.id).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      tx.problemLanguages.update(input.id) {
        input.starterCode?.let { starterCode = it }
      }.save(context).getOrThrow()

      tx.problemLanguages.query {
        where(ProblemLanguage.id eq input.id)
        loadLanguage()
      }.firstOrNull(context).getOrThrow()
    }.getOrThrow()
  }

  /** Null when absent or when the judge's admin-only read policy denies the current viewer. */
  fun findJudgeConfiguration(problemLanguageId: Long): JudgeConfiguration? {
    val viewer = currentUser.get()?.let { Viewer.User(it.id) } ?: Viewer.Anonymous

    return entClient.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
      .find(ViewerContext(viewer))
      .visibleOrNull()
      .getOrThrow()
  }

  fun createJudgeConfiguration(
    problemLanguageId: Long,
    testDriverCode: String,
    checkerSource: String?,
    timeLimitMs: Int,
    memoryLimitMb: Int,
  ): ProblemLanguage? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))

    return entClient.withTransaction { tx ->
      // Lock the language configuration so concurrent creations cannot both pass the existence check.
      tx.problemLanguages.query {
        where(ProblemLanguage.id eq problemLanguageId)
      }.forUpdate().firstOrNull(context).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      val existing = tx.judgeConfigurations.indexes.problemLanguageId(problemLanguageId)
        .find(context).getOrThrow()
      if (existing != null) {
        throw ProblemInputException("problemLanguageId", "This language already has a judge configuration")
      }

      tx.judgeConfigurations.create {
        this.problemLanguageId = problemLanguageId
        this.testDriverCode = testDriverCode
        this.checkerSource = checkerSource
        this.timeLimitMs = timeLimitMs
        this.memoryLimitMb = memoryLimitMb
      }.save(context).getOrThrow()

      tx.problemLanguages.query {
        where(ProblemLanguage.id eq problemLanguageId)
        loadLanguage()
      }.firstOrNull(context).getOrThrow()
    }.getOrThrow()
  }

  /** Null parameters leave the existing values unchanged; the checker can be cleared explicitly. */
  fun updateJudgeConfiguration(
    id: Long,
    testDriverCode: String? = null,
    checkerSource: FieldUpdate<String?> = FieldUpdate.Unchanged,
    timeLimitMs: Int? = null,
    memoryLimitMb: Int? = null,
  ): JudgeConfiguration? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))

    return entClient.withTransaction { tx ->
      val configuration = tx.judgeConfigurations.findById(context, id).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      // Judge reads are admin-only, so problem and language availability is checked through the parent.
      tx.problemLanguages.findById(context, configuration.problemLanguageId).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      tx.judgeConfigurations.update(id) {
        testDriverCode?.let { this.testDriverCode = it }
        if (checkerSource is FieldUpdate.Set) {
          this.checkerSource = checkerSource.value
        }
        timeLimitMs?.let { this.timeLimitMs = it }
        memoryLimitMb?.let { this.memoryLimitMb = it }
      }.saveAndLoad(context).getOrThrow()
    }.getOrThrow()
  }

  fun updateProblemExample(input: UpdateProblemExample): TestCase? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))

    return entClient.withTransaction { tx ->
      val example = tx.testCases.findById(context, input.id).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      if (example.visibility != TestCaseVisibility.EXAMPLE) {
        return@withTransaction null
      }

      tx.testCases.update(input.id) {
        input.inputJson?.let { inputJson = parseTestCaseJson(it, "inputJson") }
        input.expectedOutputJson?.let { expectedOutputJson = parseTestCaseJson(it, "expectedOutputJson") }
        if (input.explanationMarkdown is FieldUpdate.Set) {
          explanationMarkdown = input.explanationMarkdown.value
        }
      }.saveAndLoad(context).getOrThrow()
    }.getOrThrow()
  }

  fun deleteProblemExample(id: Long): Problem? {
    val context = ViewerContext(Viewer.User(currentUser.requireAdmin().id))

    return entClient.withTransaction { tx ->
      val example = tx.testCases.findById(context, id).visibleOrNull().getOrThrow()
        ?: return@withTransaction null

      if (example.visibility != TestCaseVisibility.EXAMPLE) {
        return@withTransaction null
      }

      if (!tx.testCases.deleteById(context, id).getOrThrow()) {
        return@withTransaction null
      }

      tx.loadProblemContent(example.problemId, context)
    }.getOrThrow()
  }

  private fun EntTransactionClient.loadProblemContent(id: Long, context: ViewerContext): Problem? =
    problems.query {
      where(Problem.id eq id)
      loadPublicContent()
    }.firstOrNull(context).getOrThrow()

  private fun parseTestCaseJson(value: String, field: String): JsonElement {
    return try {
      Json.parseToJsonElement(value)
    } catch (_: IllegalArgumentException) {
      throw ProblemInputException(field, "Provide valid JSON")
    }
  }

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

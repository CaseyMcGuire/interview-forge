package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.TestCase
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.netflix.graphql.dgs.DgsQueryExecutor
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntPrivacyDeniedException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Testcontainers
@SpringBootTest
class ProblemDataFetcherIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed fixtures in the isolated problem-query integration-test database"
  )

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var queryExecutor: DgsQueryExecutor

  @Test
  fun `lists published problems in title and slug order without unavailable problems`() {
    val prefix = UUID.randomUUID().toString()
    val last = createProblem(title = "$prefix Zebra Search")
    val first = createProblem(title = "$prefix Array Pair")
    val sameTitle = createProblem(title = "$prefix Array Pair")
    val unavailable = listOf(
      createProblem(title = "$prefix Draft", publishedAt = null),
      createProblem(title = "$prefix Archived", archivedAt = Instant.now()),
      createProblem(title = "$prefix Future", publishedAt = Instant.now().plusSeconds(3600)),
    )
    val configuration = createConfiguration(first, createLanguage("list-${UUID.randomUUID()}"))
    val example = createExample(first, position = 0)
    createExample(first, position = 1, visibility = TestCaseVisibility.HIDDEN)

    val connection = fetchConnection(mapOf("filters" to mapOf("search" to prefix)))
    val problems = connection.objects("edges").map { it.obj("node") }
    val expected = listOf(first, sameTitle).sortedBy { it.slug } + last
    assertEquals(expected.map { it.slug }, problems.map { it["slug"] })
    assertFalse(problems.any { it["slug"] in unavailable.map { problem -> problem.slug } })

    val item = problems.single { it["slug"] == first.slug }
    assertEquals("Problem:${first.id}", decodeId(item["id"]))
    assertEquals(first.title, item["title"])
    assertEquals("EASY", item["difficulty"])
    assertEquals(listOf("ProblemLanguage:${configuration.id}"), item.objects("languageConfigurations").map { decodeId(it["id"]) })
    assertEquals(listOf("ProblemExample:${example.id}"), item.objects("examples").map { decodeId(it["id"]) })
  }

  @Test
  fun `paginates tied titles with stable cursors and correct page boundaries`() {
    val prefix = UUID.randomUUID().toString()
    val sameTitle = List(3) { createProblem(title = "$prefix Alpha") }.sortedBy { it.slug }
    val last = createProblem(title = "$prefix Zulu")
    createProblem(title = "$prefix Before", publishedAt = null)
    val filters = mapOf("search" to prefix)

    val firstPage = fetchConnection(mapOf("first" to 2, "filters" to filters))
    val firstEdges = firstPage.objects("edges")
    val firstInfo = firstPage.obj("pageInfo")
    assertEquals(sameTitle.take(2).map { it.slug }, firstEdges.map { it.obj("node")["slug"] })
    assertEquals(true, firstInfo["hasNextPage"])
    assertEquals(false, firstInfo["hasPreviousPage"])
    assertEquals(firstEdges.first()["cursor"], firstInfo["startCursor"])
    assertEquals(firstEdges.last()["cursor"], firstInfo["endCursor"])

    val secondPage = fetchConnection(mapOf("first" to 2, "after" to firstInfo["endCursor"], "filters" to filters))
    val secondEdges = secondPage.objects("edges")
    val secondInfo = secondPage.obj("pageInfo")
    assertEquals(listOf(sameTitle.last().slug, last.slug), secondEdges.map { it.obj("node")["slug"] })
    assertEquals(false, secondInfo["hasNextPage"])
    assertEquals(true, secondInfo["hasPreviousPage"])
    assertEquals(secondEdges.first()["cursor"], secondInfo["startCursor"])
    assertEquals(secondEdges.last()["cursor"], secondInfo["endCursor"])
    assertEquals(4, (firstEdges + secondEdges).map { it["cursor"] }.toSet().size)

    val exhausted = fetchConnection(mapOf("first" to 2, "after" to secondInfo["endCursor"], "filters" to filters))
    assertEquals(emptyList<Any>(), exhausted["edges"])
    assertEquals(false, exhausted.obj("pageInfo")["hasNextPage"])
    assertEquals(true, exhausted.obj("pageInfo")["hasPreviousPage"])
    assertNull(exhausted.obj("pageInfo")["startCursor"])
    assertNull(exhausted.obj("pageInfo")["endCursor"])
  }

  @Test
  fun `supports empty and zero-sized pages and defaults to twenty results`() {
    val prefix = UUID.randomUUID().toString()
    repeat(21) { createProblem(title = "$prefix ${it.toString().padStart(2, '0')}") }
    val filters = mapOf("search" to prefix)
    val page = fetchConnection(mapOf("filters" to filters))
    assertEquals(20, page.objects("edges").size)
    assertEquals(true, page.obj("pageInfo")["hasNextPage"])

    val last = fetchConnection(mapOf("filters" to filters, "after" to page.obj("pageInfo")["endCursor"]))
    assertEquals(1, last.objects("edges").size)
    assertEquals(false, last.obj("pageInfo")["hasNextPage"])

    val zero = fetchConnection(mapOf("first" to 0, "filters" to filters))
    assertEquals(emptyList<Any>(), zero["edges"])
    assertEquals(true, zero.obj("pageInfo")["hasNextPage"])
    assertEquals(false, zero.obj("pageInfo")["hasPreviousPage"])
    assertNull(zero.obj("pageInfo")["startCursor"])
    assertNull(zero.obj("pageInfo")["endCursor"])

    val empty = fetchConnection(mapOf("filters" to mapOf("search" to "missing-${UUID.randomUUID()}")))
    assertEquals(emptyList<Any>(), empty["edges"])
    assertEquals(false, empty.obj("pageInfo")["hasNextPage"])
    assertEquals(false, empty.obj("pageInfo")["hasPreviousPage"])
    assertNull(empty.obj("pageInfo")["startCursor"])
    assertNull(empty.obj("pageInfo")["endCursor"])
  }

  @Test
  fun `combines optional filters before pagination and treats search wildcards literally`() {
    val prefix = UUID.randomUUID().toString()
    createProblem(title = "$prefix Alpha", difficulty = ProblemDifficulty.HARD)
    val first = createProblem(title = "$prefix Beta", difficulty = ProblemDifficulty.MEDIUM)
    val second = createProblem(title = "$prefix Gamma", difficulty = ProblemDifficulty.MEDIUM)
    val filters = mapOf("search" to "  $prefix  ", "difficulty" to "MEDIUM")
    val page = fetchConnection(mapOf("first" to 1, "filters" to filters))
    assertEquals(listOf(first.slug), page.objects("edges").map { it.obj("node")["slug"] })
    val next = fetchConnection(mapOf("first" to 1, "filters" to filters, "after" to page.obj("pageInfo")["endCursor"]))
    assertEquals(listOf(second.slug), next.objects("edges").map { it.obj("node")["slug"] })
    assertEquals(false, next.obj("pageInfo")["hasNextPage"])

    val literal = createProblem(title = "$prefix 100%_done")
    createProblem(title = "$prefix 100x_done")
    val literalPage = fetchConnection(mapOf("filters" to mapOf("search" to "$prefix 100%_")))
    assertEquals(listOf(literal.slug), literalPage.objects("edges").map { it.obj("node")["slug"] })

    val unfiltered = fetchConnection(mapOf("first" to 100, "filters" to null))
    val blankSearch = fetchConnection(mapOf("first" to 100, "filters" to mapOf("search" to " ")))
    assertEquals(unfiltered, blankSearch)
    val difficultyOnly = fetchConnection(mapOf("first" to 100, "filters" to mapOf("difficulty" to "MEDIUM")))
    assertTrue(difficultyOnly.objects("edges").all { it.obj("node")["difficulty"] == "MEDIUM" })
    assertTrue(difficultyOnly.objects("edges").any { it.obj("node")["slug"] == first.slug })
  }

  @Test
  fun `rejects invalid page sizes and malformed cursors as bad requests`() {
    val invalidCursors = listOf(
      "",
      "not-base64!",
      Base64.getUrlEncoder().encodeToString("not json".toByteArray()),
      Base64.getUrlEncoder().encodeToString("[\"wrong-connection\",\"Title\",\"slug\"]".toByteArray()),
      Base64.getUrlEncoder().encodeToString("[\"problems:v1\",123,\"slug\"]".toByteArray()),
    )
    val invalidArguments = listOf(mapOf("first" to -1), mapOf("first" to 101)) +
      invalidCursors.map { mapOf("after" to it) }
    invalidArguments.forEach { arguments ->
      val result = queryExecutor.execute(PROBLEMS_QUERY, arguments)
      assertTrue(result.errors.isNotEmpty(), arguments.toString())
      assertEquals("BAD_REQUEST", result.errors.first().extensions["errorType"].toString())
    }
  }

  @Test
  fun `returns public problem content with ordered examples and enabled language stencils`() {
    val problem = createProblem()
    val python = createConfiguration(problem, createLanguage("python"))
    val kotlinLanguage = entClient.languages.query {
      where(Language.key eq "kotlin")
    }.firstOrNull(fixtureContext).getOrThrow()!!
    val kotlin = createConfiguration(problem, kotlinLanguage)
    val java = createLanguage("java")
    createConfiguration(problem, java)
    entClient.languages.update(java.id) { enabled = false }.save(fixtureContext).getOrThrow()
    val later = createExample(problem, position = 8, expected = "null")
    val first = createExample(problem, position = 2, explanation = "Use two different positions.")
    val hidden = createExample(problem, position = 1, visibility = TestCaseVisibility.HIDDEN)
    createExample(createProblem(), position = 0)
    entClient.judgeConfigurations.create {
      problemLanguageId = kotlin.id
      runtime = "kotlin-test"
      testDriverCode = "private test driver code"
      checkerSource = "private checker source"
      timeLimitMs = 1000
      memoryLimitMb = 256
    }.save(fixtureContext).getOrThrow()

    val result = requireNotNull(fetchProblem(problem.slug))
    assertEquals("Problem:${problem.id}", decodeId(result["id"]))
    assertEquals(problem.slug, result["slug"])
    assertEquals("Two Sum", result["title"])
    assertEquals("Find two indices.\n\n## Constraints\nUse different positions.", result["statementMarkdown"])
    assertEquals("EASY", result["difficulty"])

    val configurations = result.objects("languageConfigurations")
    assertEquals(listOf("kotlin", "python"), configurations.map { it.obj("language")["key"] })
    assertEquals(listOf(kotlin.id, python.id), configurations.map { decodeId(it["id"]).substringAfter(':').toLong() })
    assertEquals("ProblemLanguage:${kotlin.id}", decodeId(configurations.first()["id"]))
    assertEquals(kotlin.starterCode, configurations.first()["starterCode"])
    assertEquals("Kotlin", configurations.first().obj("language")["displayName"])
    assertEquals("Language:${kotlin.languageId}", decodeId(configurations.first().obj("language")["id"]))

    val examples = result.objects("examples")
    assertEquals(listOf(2, 8), examples.map { it["position"] })
    assertEquals(listOf("ProblemExample:${first.id}", "ProblemExample:${later.id}"), examples.map { decodeId(it["id"]) })
    assertEquals(first.inputJson, Json.parseToJsonElement(examples.first()["inputJson"] as String))
    assertEquals(first.expectedOutputJson, Json.parseToJsonElement(examples.first()["expectedOutputJson"] as String))
    assertEquals(first.explanationMarkdown, examples.first()["explanationMarkdown"])
    assertEquals("null", examples.last()["expectedOutputJson"])
    assertEquals(JsonNull, Json.parseToJsonElement(examples.last()["expectedOutputJson"] as String))
    assertNull(examples.last()["explanationMarkdown"])
    assertFalse(examples.any { decodeId(it["id"]) == "ProblemExample:${hidden.id}" })
    assertFalse(result.toString().contains("private test driver code"))
    assertFalse(result.toString().contains("private checker source"))
  }

  @Test
  fun `returns null without errors for missing draft archived and future problems`() {
    assertNull(fetchProblem("missing-${UUID.randomUUID()}"))
    val unavailable = listOf(
      createProblem(publishedAt = null),
      createProblem(archivedAt = Instant.now()),
      createProblem(publishedAt = Instant.now().plusSeconds(3600)),
    )
    unavailable.forEach {
      createExample(it, position = 0)
      assertNull(fetchProblem(it.slug))
    }
  }

  @Test
  fun `returns empty lists when there are no public examples or enabled configurations`() {
    val problem = createProblem()
    createExample(problem, position = 0, visibility = TestCaseVisibility.HIDDEN)
    val language = createLanguage("disabled-${UUID.randomUUID()}")
    createConfiguration(problem, language)
    entClient.languages.update(language.id) { enabled = false }.save(fixtureContext).getOrThrow()

    val result = requireNotNull(fetchProblem(problem.slug))
    assertEquals(emptyList<Any>(), result["examples"])
    assertEquals(emptyList<Any>(), result["languageConfigurations"])
  }

  @Test
  fun `read policies protect private rows for anonymous visitors authors and other users`() {
    val publicProblem = createProblem()
    val draft = createProblem(publishedAt = null)
    val example = createExample(publicProblem, position = 0)
    val hidden = createExample(publicProblem, position = 1, visibility = TestCaseVisibility.HIDDEN)
    val draftExample = createExample(draft, position = 0)
    val enabledLanguage = createLanguage("enabled-${UUID.randomUUID()}")
    val disabledLanguage = createLanguage("disabled-${UUID.randomUUID()}")
    val publicConfiguration = createConfiguration(publicProblem, enabledLanguage)
    val draftConfiguration = createConfiguration(draft, enabledLanguage)
    val disabledConfiguration = createConfiguration(publicProblem, disabledLanguage)
    entClient.languages.update(disabledLanguage.id) { enabled = false }.save(fixtureContext).getOrThrow()

    for (viewer in listOf(Viewer.Anonymous, Viewer.User(publicProblem.createdByUserId), Viewer.User(draft.createdByUserId))) {
      val context = ViewerContext(viewer)
      assertEquals(publicProblem.id, entClient.problems.findById(context, publicProblem.id).getOrThrow()?.id)
      assertEquals(example.id, entClient.testCases.findById(context, example.id).getOrThrow()?.id)
      assertEquals(publicConfiguration.id, entClient.problemLanguages.findById(context, publicConfiguration.id).getOrThrow()?.id)
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.problems.findById(context, draft.id).getOrThrow()
      }
      for (testCase in listOf(hidden, draftExample)) {
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.testCases.findById(context, testCase.id).getOrThrow()
        }
      }
      for (configuration in listOf(draftConfiguration, disabledConfiguration)) {
        assertThrows(EntPrivacyDeniedException::class.java) {
          entClient.problemLanguages.findById(context, configuration.id).getOrThrow()
        }
      }
      assertThrows(EntPrivacyDeniedException::class.java) {
        entClient.languages.findById(context, disabledLanguage.id).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.problems.update(publicProblem.id) { title = "Unauthorized edit" }.save(context).getOrThrow()
      }
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.testCases.deleteById(context, example.id).getOrThrow()
      }
    }
  }

  private fun fetchProblem(slug: String): Map<String, Any?>? {
    val result = queryExecutor.execute(PROBLEM_QUERY, mapOf("slug" to slug))
    assertTrue(result.errors.isEmpty(), result.errors.toString())
    return result.getData<Map<String, Map<String, Any?>?>>()?.get("problem")
  }

  private fun fetchConnection(arguments: Map<String, Any?> = emptyMap()): Map<String, Any?> {
    val result = queryExecutor.execute(PROBLEMS_QUERY, arguments)
    assertTrue(result.errors.isEmpty(), result.errors.toString())
    return result.getData<Map<String, Any?>>()!!.obj("problems")
  }

  private fun createProblem(
    publishedAt: Instant? = Instant.now().minusSeconds(60),
    archivedAt: Instant? = null,
    title: String = "Two Sum",
    difficulty: ProblemDifficulty = ProblemDifficulty.EASY,
  ): Problem {
    val author = entClient.users.create {
      email = "${UUID.randomUUID()}@example.com"
      hashedPassword = "test-only-hash"
    }.saveAndLoad(fixtureContext).getOrThrow()
    return entClient.problems.create {
      slug = "problem-${UUID.randomUUID()}"
      this.title = title
      statementMarkdown = "Find two indices.\n\n## Constraints\nUse different positions."
      this.difficulty = difficulty
      createdByUserId = author.id
      this.publishedAt = publishedAt
      this.archivedAt = archivedAt
    }.saveAndLoad(fixtureContext).getOrThrow()
  }

  private fun createLanguage(key: String, enabled: Boolean = true): Language =
    entClient.languages.create {
      this.key = key
      displayName = "$key display name"
      this.enabled = enabled
    }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createConfiguration(problem: Problem, language: Language): ProblemLanguage =
    entClient.problemLanguages.create {
      problemId = problem.id
      languageId = language.id
      starterCode = "// ${language.key} starter code"
    }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createExample(
    problem: Problem,
    position: Int,
    visibility: TestCaseVisibility = TestCaseVisibility.EXAMPLE,
    expected: String = "[0,3]",
    explanation: String? = null,
  ): TestCase = entClient.testCases.create {
    problemId = problem.id
    this.position = position
    this.visibility = visibility
    inputJson = Json.parseToJsonElement("""{"nums":[4,8,1,6],"target":10}""")
    expectedOutputJson = Json.parseToJsonElement(expected)
    explanationMarkdown = explanation
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun decodeId(value: Any?): String = String(Base64.getUrlDecoder().decode(value as String))

  @Suppress("UNCHECKED_CAST")
  private fun Map<String, Any?>.objects(key: String): List<Map<String, Any?>> = get(key) as List<Map<String, Any?>>

  @Suppress("UNCHECKED_CAST")
  private fun Map<String, Any?>.obj(key: String): Map<String, Any?> = get(key) as Map<String, Any?>

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))

    private val PROBLEMS_QUERY = """
      query ListProblems(${'$'}first: Int, ${'$'}after: String, ${'$'}filters: ProblemFilterInput) {
        problems(first: ${'$'}first, after: ${'$'}after, filters: ${'$'}filters) {
          edges {
            cursor
            node {
              id slug title difficulty
              languageConfigurations { id }
              examples { id }
            }
          }
          pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
        }
      }
    """.trimIndent()

    private val PROBLEM_QUERY = """
      query ViewProblem(${'$'}slug: String!) {
        problem(slug: ${'$'}slug) {
          id slug title statementMarkdown difficulty
          languageConfigurations {
            id starterCode
            language { id key displayName }
          }
          examples { id position inputJson expectedOutputJson explanationMarkdown }
        }
      }
    """.trimIndent()
  }
}

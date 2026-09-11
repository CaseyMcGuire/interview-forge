package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.TestCase
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.netflix.graphql.dgs.DgsQueryExecutor
import entkt.postgres.PostgresDriver
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
import javax.sql.DataSource

@Testcontainers
@SpringBootTest(properties = ["spring.flyway.enabled=false"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProblemDataFetcherIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed fixtures in the isolated problem-query integration-test database"
  )

  @Autowired
  lateinit var entClient: EntClient

  @Autowired
  lateinit var queryExecutor: DgsQueryExecutor

  @Autowired
  lateinit var dataSource: DataSource

  @BeforeAll
  fun createTestTables() {
    // Draft schemas have no production migration yet. Auto-DDL is confined to this throwaway DB.
    PostgresDriver(dataSource, autoDdl = true).registerAll(EntClient.SCHEMAS)
  }

  @Test
  fun `returns public problem content with ordered examples and enabled language stencils`() {
    val problem = createProblem()
    val python = createConfiguration(problem, createLanguage("python"))
    val kotlin = createConfiguration(problem, createLanguage("kotlin"))
    createConfiguration(problem, createLanguage("java", enabled = false))
    val later = createExample(problem, position = 8, expected = "null")
    val first = createExample(problem, position = 2, explanation = "Use two different positions.")
    val hidden = createExample(problem, position = 1, visibility = TestCaseVisibility.HIDDEN)
    createExample(createProblem(), position = 0)
    entClient.judgeConfigurations.create {
      problemLanguageId = kotlin.id
      runtimeKey = "kotlin-test"
      harnessSource = "private harness source"
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
    assertEquals(kotlin.solutionFilename, configurations.first()["solutionFilename"])
    assertEquals("kotlin display name", configurations.first().obj("language")["displayName"])
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
    assertFalse(result.toString().contains("private harness source"))
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
    createConfiguration(problem, createLanguage("disabled-${UUID.randomUUID()}", enabled = false))

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
    val disabledLanguage = createLanguage("disabled-${UUID.randomUUID()}", enabled = false)
    val publicConfiguration = createConfiguration(publicProblem, enabledLanguage)
    val draftConfiguration = createConfiguration(draft, enabledLanguage)
    val disabledConfiguration = createConfiguration(publicProblem, disabledLanguage)

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

  private fun createProblem(
    publishedAt: Instant? = Instant.now().minusSeconds(60),
    archivedAt: Instant? = null,
  ): Problem {
    val author = entClient.users.create {
      email = "${UUID.randomUUID()}@example.com"
      hashedPassword = "test-only-hash"
    }.saveAndLoad(fixtureContext).getOrThrow()
    return entClient.problems.create {
      slug = "problem-${UUID.randomUUID()}"
      title = "Two Sum"
      statementMarkdown = "Find two indices.\n\n## Constraints\nUse different positions."
      difficulty = ProblemDifficulty.EASY
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
      solutionFilename = if (language.key == "python") "solution.py" else "Solution.kt"
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

    private val PROBLEM_QUERY = """
      query ViewProblem(${'$'}slug: String!) {
        problem(slug: ${'$'}slug) {
          id slug title statementMarkdown difficulty
          languageConfigurations {
            id starterCode solutionFilename
            language { id key displayName }
          }
          examples { id position inputJson expectedOutputJson explanationMarkdown }
        }
      }
    """.trimIndent()
  }
}

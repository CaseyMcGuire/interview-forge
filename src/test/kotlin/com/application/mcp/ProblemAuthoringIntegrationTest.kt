package com.application.mcp

import com.application.ent.EntClient
import com.application.ent.Problem
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import entkt.runtime.privacy.ViewerContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = ["mcp.api-token=${McpServerIntegrationTest.TEST_TOKEN}", "mcp.user-id=1"],
)
class ProblemAuthoringIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect MCP authoring fixtures in an isolated test database",
  )
  private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var mapper: ObjectMapper
  @LocalServerPort private var port = 0

  @BeforeAll
  fun createAdministrator() {
    val administrator = entClient.users.create {
      email = "author@example.com"
      hashedPassword = "unused"
      role = UserRole.ADMIN
    }.saveAndLoad(fixtures).getOrThrow()
    assertEquals(1L, administrator.id)

    entClient.languages.create {
      key = "disabled"
      displayName = "Disabled"
      enabled = false
    }.save(fixtures).getOrThrow()
  }

  @Test
  fun `create publishes complete content with ordered cases and exact JSON values`() {
    val input = problemInput()
    val created = callTool("create_problem", input)
    assertFalse(created["isError"].asBoolean(), created.toString())
    assertEquals(input["slug"], created["structuredContent"]["problem"]["slug"].asString())

    val problem = inspectProblem(input["slug"] as String)
    assertEquals("Echo a number", problem["title"].asString())
    assertEquals("EASY", problem["difficulty"].asString())
    val language = problem["languageConfigurations"][0]
    assertEquals(configuration()["starterCode"], language["starterCode"].asString())
    assertEquals(configuration()["referenceSolutionCode"], language["judgeConfiguration"]["referenceSolutionCode"].asString())
    assertEquals(1000, language["judgeConfiguration"]["timeLimitMs"].asInt())
    assertEquals("9007199254740993", problem["publicExamples"][0]["inputJson"].asString())
    assertEquals("9007199254740993", problem["publicExamples"][0]["expectedOutputJson"].asString())
    assertEquals(listOf(1, 2), problem["testCases"].toList().map { it["position"].asInt() })
    assertEquals("null", problem["testCases"][1]["expectedOutputJson"].asString())

    val stored = requireNotNull(findProblem(input["slug"] as String))
    assertEquals(1L, stored.createdByUserId)
    assertNotNull(stored.publishedAt)
    val cases = entClient.testCases.indexes.problemId(stored.id).query().all(fixtures).getOrThrow()
    assertEquals(1, cases.count { it.visibility == TestCaseVisibility.EXAMPLE })
    assertEquals(2, cases.count { it.visibility == TestCaseVisibility.HIDDEN })
  }

  @Test
  fun `invalid content returns field errors and rolls back the entire creation`() {
    val invalidInputs = listOf(
      problemInput() + ("title" to " ") to "title",
      problemInput() + ("languageConfigurations" to listOf(configuration() + ("timeLimitMs" to 0))) to "timeLimitMs",
      problemInput() + ("languageConfigurations" to listOf(configuration() + ("referenceSolutionCode" to " "))) to "referenceSolutionCode",
      problemInput() + ("testCases" to listOf(case("1"), case("{broken"))) to "testCases[1].inputJson",
      problemInput() + ("testCases" to listOf(case("1", "1 2"))) to "testCases[0].expectedOutputJson",
      problemInput() + ("publicExamples" to listOf(case("{\"x\":1,\"x\":2}"))) to "publicExamples[0].inputJson",
      problemInput() + ("testCases" to listOf(case("\"${"x".repeat(20_000)}\""))) to "inputJson",
    )

    val languageCount = entClient.problemLanguages.query().all(fixtures).getOrThrow().size
    val judgeCount = entClient.judgeConfigurations.query().all(fixtures).getOrThrow().size
    val caseCount = entClient.testCases.query().all(fixtures).getOrThrow().size

    for ((input, field) in invalidInputs) {
      val result = callTool("create_problem", input)
      assertTrue(result["isError"].asBoolean(), result.toString())
      assertEquals(field, result["structuredContent"]["errors"][0]["field"].asString(), result.toString())
      assertNull(findProblem(input["slug"] as String))
    }

    assertEquals(languageCount, entClient.problemLanguages.query().all(fixtures).getOrThrow().size)
    assertEquals(judgeCount, entClient.judgeConfigurations.query().all(fixtures).getOrThrow().size)
    assertEquals(caseCount, entClient.testCases.query().all(fixtures).getOrThrow().size)
  }

  @Test
  fun `duplicate slugs and invalid collections cannot overwrite or partially create problems`() {
    val original = problemInput()
    assertFalse(callTool("create_problem", original)["isError"].asBoolean())
    val duplicate = callTool("create_problem", original + ("title" to "Replacement"))
    assertTrue(duplicate["isError"].asBoolean())
    assertEquals("slug", duplicate["structuredContent"]["errors"][0]["field"].asString())
    assertEquals("Echo a number", inspectProblem(original["slug"] as String)["title"].asString())

    val invalidCollections = listOf(
      "languageConfigurations" to emptyList<Any>(),
      "languageConfigurations" to listOf(configuration(), configuration()),
      "languageConfigurations" to listOf(configuration("disabled")),
      "languageConfigurations" to listOf(configuration("missing-language")),
      "publicExamples" to emptyList<Any>(),
      "testCases" to List(101) { case("1") },
    )
    for (invalid in invalidCollections) {
      val input = problemInput() + invalid
      assertTrue(callTool("create_problem", input)["isError"].asBoolean())
      assertNull(findProblem(input["slug"] as String))
    }
  }

  @Test
  fun `metadata updates preserve omitted fields and missing slugs produce field errors`() {
    val input = problemInput() - "testCases"
    assertFalse(callTool("create_problem", input)["isError"].asBoolean())
    val slug = input["slug"] as String

    val updated = callTool("update_problem", mapOf("slug" to slug, "title" to " Renamed "))
    assertFalse(updated["isError"].asBoolean(), updated.toString())
    val problem = inspectProblem(slug)
    assertEquals("Renamed", problem["title"].asString())
    assertEquals(input["statementMarkdown"], problem["statementMarkdown"].asString())
    assertEquals("EASY", problem["difficulty"].asString())
    assertEquals(1, problem["languageConfigurations"].size())
    assertEquals(1, problem["publicExamples"].size())
    assertEquals(0, problem["testCases"].size())

    val invalid = callTool("update_problem", mapOf("slug" to slug, "title" to "Changed", "statementMarkdown" to " "))
    assertTrue(invalid["isError"].asBoolean())
    assertEquals("Renamed", inspectProblem(slug)["title"].asString())

    val missing = callTool("update_problem", mapOf("slug" to "missing", "title" to "Changed"))
    assertTrue(missing["isError"].asBoolean())
    assertEquals("slug", missing["structuredContent"]["errors"][0]["field"].asString())
  }

  @Test
  fun `language configuration adds or replaces starter and judge code atomically`() {
    val input = problemInput()
    assertFalse(callTool("create_problem", input)["isError"].asBoolean())
    val slug = input["slug"] as String
    val original = inspectProblem(slug)["languageConfigurations"][0]

    val invalid = configuration() + mapOf("starterCode" to "class Changed", "timeLimitMs" to 0)
    assertTrue(callTool("configure_problem_language", mapOf("slug" to slug, "configuration" to invalid))["isError"].asBoolean())
    assertEquals(original, inspectProblem(slug)["languageConfigurations"][0])

    val replacement = configuration() + mapOf("starterCode" to "class Changed", "referenceSolutionCode" to "class Corrected", "timeLimitMs" to 2000)
    repeat(2) {
      val result = callTool("configure_problem_language", mapOf("slug" to slug, "configuration" to replacement))
      assertFalse(result["isError"].asBoolean(), result.toString())
    }
    val replaced = inspectProblem(slug)["languageConfigurations"]
    assertEquals(1, replaced.size())
    assertEquals("class Changed", replaced[0]["starterCode"].asString())
    assertEquals("class Corrected", replaced[0]["judgeConfiguration"]["referenceSolutionCode"].asString())
    assertEquals(2000, replaced[0]["judgeConfiguration"]["timeLimitMs"].asInt())

    entClient.languages.create {
      key = "another-language"
      displayName = "Another language"
      enabled = true
    }.save(fixtures).getOrThrow()
    val added = callTool("configure_problem_language", mapOf("slug" to slug, "configuration" to configuration("another-language")))
    assertFalse(added["isError"].asBoolean(), added.toString())
    assertEquals(2, inspectProblem(slug)["languageConfigurations"].size())
  }

  @Test
  fun `test batches append in order and an invalid later case rolls back the entire batch`() {
    val input = problemInput()
    assertFalse(callTool("create_problem", input)["isError"].asBoolean())
    val slug = input["slug"] as String

    val added = callTool("add_test_cases", mapOf(
      "slug" to slug,
      "publicExamples" to listOf(case("1.0")),
      "testCases" to listOf(case("9007199254740993"), case("null")),
    ))
    assertFalse(added["isError"].asBoolean(), added.toString())
    assertEquals(3, added["structuredContent"]["publicExamples"][0]["position"].asInt())
    val tests = added["structuredContent"]["testCases"].toList()
    assertEquals(listOf(4, 5), tests.map { it["position"].asInt() })
    assertEquals("9007199254740993", tests[0]["inputJson"].asString())
    assertEquals("null", tests[1]["expectedOutputJson"].asString())

    val before = inspectProblem(slug)
    val invalid = callTool("add_test_cases", mapOf(
      "slug" to slug,
      "publicExamples" to listOf(case("2")),
      "testCases" to listOf(case("3"), case("broken")),
    ))
    assertTrue(invalid["isError"].asBoolean())
    assertEquals("testCases[1].inputJson", invalid["structuredContent"]["errors"][0]["field"].asString())
    assertEquals(before, inspectProblem(slug))

    for (arguments in listOf(
      mapOf("slug" to slug),
      mapOf("slug" to slug, "testCases" to List(101) { case("1") }),
      mapOf("slug" to slug, "publicExamples" to List(21) { case("1") }),
      mapOf("slug" to "missing", "testCases" to listOf(case("1"))),
    )) {
      assertTrue(callTool("add_test_cases", arguments)["isError"].asBoolean())
    }
    assertEquals(before, inspectProblem(slug))
  }

  @Test
  fun `case updates support both visibilities and preserve identity and position`() {
    val input = problemInput()
    assertFalse(callTool("create_problem", input)["isError"].asBoolean())
    val slug = input["slug"] as String
    val problem = inspectProblem(slug)

    for (field in listOf("publicExamples", "testCases")) {
      val original = problem[field][0]
      val id = original["id"].asString()
      val updated = callTool("update_test_case", mapOf(
        "testCaseId" to id,
        "inputJson" to "1.0",
        "expectedOutputJson" to "null",
        "explanationMarkdown" to "Revised case",
      ))
      assertFalse(updated["isError"].asBoolean(), updated.toString())
      val stored = inspectProblem(slug)[field][0]
      assertEquals(id, stored["id"].asString())
      assertEquals(original["position"], stored["position"])
      assertEquals("1.0", stored["inputJson"].asString())
      assertEquals("null", stored["expectedOutputJson"].asString())

      val cleared = callTool("update_test_case", mapOf(
        "testCaseId" to id, "inputJson" to "2", "expectedOutputJson" to "2",
      ))
      assertFalse(cleared["isError"].asBoolean())
      assertTrue(inspectProblem(slug)[field][0]["explanationMarkdown"].isNull)

      val invalid = callTool("update_test_case", mapOf(
        "testCaseId" to id, "inputJson" to "3", "expectedOutputJson" to "1 2",
      ))
      assertTrue(invalid["isError"].asBoolean())
      assertEquals("2", inspectProblem(slug)[field][0]["inputJson"].asString())
    }

    for (id in listOf("invalid", "0", Long.MAX_VALUE.toString())) {
      val result = callTool("update_test_case", mapOf(
        "testCaseId" to id, "inputJson" to "null", "expectedOutputJson" to "null",
      ))
      assertTrue(result["isError"].asBoolean())
      assertEquals("testCaseId", result["structuredContent"]["errors"][0]["field"].asString())
    }
  }

  @Test
  fun `an invalid token or revoked administrator cannot write content`() {
    val input = problemInput()
    val params = mapOf("name" to "create_problem", "arguments" to input)
    assertEquals(401, sendRpc("tools/call", params, "invalid-token").statusCode())

    entClient.users.update(1) { role = UserRole.USER }.save(fixtures).getOrThrow()
    try {
      assertEquals(403, sendRpc("tools/call", params).statusCode())
    } finally {
      entClient.users.update(1) { role = UserRole.ADMIN }.save(fixtures).getOrThrow()
    }

    assertNull(findProblem(input["slug"] as String))
  }

  private fun problemInput(): Map<String, Any> = mapOf(
    "slug" to "mcp-author-${UUID.randomUUID()}",
    "title" to " Echo a number ",
    "statementMarkdown" to "Return the supplied number unchanged.",
    "difficulty" to "EASY",
    "languageConfigurations" to listOf(configuration()),
    "publicExamples" to listOf(case("9007199254740993")),
    "testCases" to listOf(case("0"), case("null")),
  )

  private fun configuration(languageKey: String = "kotlin"): Map<String, Any> = mapOf(
    "languageKey" to languageKey,
    "starterCode" to "class Solution { fun solve(input: Long): Long = TODO() }",
    "testDriverCode" to "fun main() { print(Solution().solve(System.`in`.bufferedReader().readText().toLong())) }",
    "referenceSolutionCode" to "class Solution { fun solve(input: Long): Long = input }",
    "timeLimitMs" to 1000,
    "memoryLimitMb" to 256,
  )

  private fun case(input: String, expected: String = input) = mapOf("inputJson" to input, "expectedOutputJson" to expected)

  private fun findProblem(slug: String): Problem? = entClient.problems.indexes.slug(slug).find(fixtures).getOrThrow()

  private fun inspectProblem(slug: String): JsonNode {
    val result = callTool("get_problem", mapOf("slug" to slug))
    assertFalse(result["isError"].asBoolean(), result.toString())
    return result["structuredContent"]["problem"]
  }

  private fun callTool(name: String, arguments: Map<String, Any>): JsonNode {
    val response = sendRpc("tools/call", mapOf("name" to name, "arguments" to arguments))
    assertEquals(200, response.statusCode(), response.body())
    val body = mapper.readTree(response.body())
    assertFalse(body.has("error"), body.toString())
    return body["result"]
  }

  private fun sendRpc(
    method: String,
    params: Map<String, Any>,
    token: String = McpServerIntegrationTest.TEST_TOKEN,
  ): HttpResponse<String> {
    val request = HttpRequest.newBuilder(URI("http://localhost:$port/mcp"))
      .timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json, text/event-stream")
      .header("MCP-Protocol-Version", "2025-11-25")
      .header("Authorization", "Bearer $token")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(mapOf(
        "jsonrpc" to "2.0", "id" to 1, "method" to method, "params" to params,
      ))))
      .build()

    return http.send(request, HttpResponse.BodyHandlers.ofString())
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

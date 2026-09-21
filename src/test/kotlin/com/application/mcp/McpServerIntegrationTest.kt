package com.application.mcp

import com.application.db.models.UserDetailsImpl
import com.application.ent.EntClient
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.services.User
import entkt.runtime.privacy.ViewerContext
import jakarta.servlet.Filter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
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
import java.time.Instant

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = ["mcp.api-token=${McpServerIntegrationTest.TEST_TOKEN}", "mcp.user-id=1"],
)
class McpServerIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS(
    "Seed MCP catalog and authorization fixtures in an isolated test database",
  )
  private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var mapper: ObjectMapper
  @Autowired lateinit var applicationContext: WebApplicationContext
  @Autowired @Qualifier("springSecurityFilterChain") lateinit var securityFilter: Filter
  @LocalServerPort private var port = 0

  @BeforeAll
  fun createCatalog() {
    val administrator = entClient.users.create {
      email = "mcp-admin@example.com"
      hashedPassword = "unused"
      role = UserRole.ADMIN
    }.saveAndLoad(fixtures).getOrThrow()
    assertEquals(1L, administrator.id)

    val language = entClient.languages.create {
      key = "mcp-kotlin"
      displayName = "MCP Kotlin"
      enabled = true
    }.saveAndLoad(fixtures).getOrThrow()
    entClient.languages.create {
      key = "mcp-disabled"
      displayName = "Disabled language"
      enabled = false
    }.save(fixtures).getOrThrow()

    for ((slug, title) in listOf(
      "mcp-alpha" to "Catalog Alpha",
      "mcp-beta" to "Catalog Beta",
      "mcp-archived" to "Catalog Archived",
      "mcp-draft" to "Catalog Draft",
    )) {
      val problem = entClient.problems.create {
        this.slug = slug
        this.title = title
        statementMarkdown = "Compare three cards."
        difficulty = ProblemDifficulty.MEDIUM
        createdByUserId = administrator.id
        publishedAt = if (slug == "mcp-draft") null else Instant.now()
        archivedAt = if (slug == "mcp-archived") Instant.now() else null
      }.saveAndLoad(fixtures).getOrThrow()

      val configuration = entClient.problemLanguages.create {
        problemId = problem.id
        languageId = language.id
        starterCode = "class Solution"
      }.saveAndLoad(fixtures).getOrThrow()
      entClient.judgeConfigurations.create {
        problemLanguageId = configuration.id
        testDriverCode = "private driver"
        referenceSolutionCode = "private reference solution"
        timeLimitMs = 1000
        memoryLimitMb = 256
      }.save(fixtures).getOrThrow()

      for ((position, visibility) in listOf(TestCaseVisibility.EXAMPLE, TestCaseVisibility.HIDDEN).withIndex()) {
        entClient.testCases.create {
          problemId = problem.id
          this.position = position
          this.visibility = visibility
          inputJson = Json.parseToJsonElement("9007199254740993")
          expectedOutputJson = JsonNull
        }.save(fixtures).getOrThrow()
      }
    }
  }

  @Test
  fun `a client can initialize and distinguish catalog tools from authoring tools`() {
    val initialized = rpc("initialize", mapOf(
      "protocolVersion" to "2025-11-25",
      "capabilities" to emptyMap<String, Any>(),
      "clientInfo" to mapOf("name" to "integration-test", "version" to "1.0"),
    ))
    assertEquals("interview-forge", initialized["serverInfo"]["name"].asString())
    assertTrue(initialized["capabilities"].has("tools"))
    assertFalse(initialized["capabilities"].has("resources"))

    val tools = rpc("tools/list")["tools"].toList()
    val catalogTools = setOf("list_languages", "search_problems", "get_problem", "get_problem_submission")
    val authoringTools = setOf(
      "create_problem", "update_problem", "configure_problem_language", "add_test_cases", "update_test_case",
      "generate_expected_output", "enqueue_problem_submission",
    )
    assertEquals(catalogTools + authoringTools, tools.map { it["name"].asString() }.toSet())
    for (tool in tools) {
      val isCatalogTool = tool["name"].asString() in catalogTools
      assertEquals(isCatalogTool, tool["annotations"]["readOnlyHint"].asBoolean())
      assertFalse(tool["annotations"]["openWorldHint"].asBoolean())
      assertEquals("object", tool["inputSchema"]["type"].asString())
      if (isCatalogTool) {
        assertEquals("object", tool["outputSchema"]["type"].asString())
      }
    }
  }

  @Test
  fun `language discovery excludes disabled languages`() {
    val result = callTool("list_languages")
    val keys = result["languages"].toList().map { it["key"].asString() }
    assertTrue(keys.contains("mcp-kotlin"))
    assertFalse(keys.contains("mcp-disabled"))
  }

  @Test
  fun `search supports filters and stable pagination without returning private content`() {
    val arguments = mapOf("search" to "Catalog", "difficulty" to "MEDIUM", "first" to 1)
    val first = callTool("search_problems", arguments)
    assertEquals(listOf("mcp-alpha"), first["problems"].toList().map { it["slug"].asString() })
    assertFalse(first.toString().contains("private reference solution"))

    val second = callTool("search_problems", arguments + ("after" to first["nextCursor"].asString()))
    assertEquals(listOf("mcp-beta"), second["problems"].toList().map { it["slug"].asString() })
    assertTrue(second["nextCursor"].isNull)
    assertEquals(0, callTool("search_problems", mapOf("difficulty" to "HARD"))["problems"].size())
    assertEquals(2, callTool("search_problems")["problems"].size())
  }

  @Test
  fun `problem inspection retains administrator access through the MCP handler`() {
    val problem = callTool("get_problem", mapOf("slug" to "mcp-alpha"))["problem"]
    val configuration = problem["languageConfigurations"][0]
    assertEquals("Compare three cards.", problem["statementMarkdown"].asString())
    assertEquals("class Solution", configuration["starterCode"].asString())
    assertEquals("private reference solution", configuration["judgeConfiguration"]["referenceSolutionCode"].asString())
    assertEquals("private driver", configuration["judgeConfiguration"]["testDriverCode"].asString())
    assertEquals(1, problem["publicExamples"].size())
    assertEquals(1, problem["testCases"].size())
    assertEquals("9007199254740993", problem["testCases"][0]["inputJson"].asString())
    assertEquals("null", problem["testCases"][0]["expectedOutputJson"].asString())
    assertFalse(problem.toString().contains("hashedPassword"))

    for (slug in listOf("not-found", "mcp-archived", "mcp-draft")) {
      assertTrue(callTool("get_problem", mapOf("slug" to slug))["problem"].isNull)
    }
  }

  @Test
  fun `invalid tool arguments return errors that the caller can correct`() {
    for ((arguments, message) in listOf(
      mapOf("first" to 0) to "first must be between 1 and 100",
      mapOf("first" to 101) to "first must be between 1 and 100",
      mapOf("after" to "not-a-cursor") to "after must be a cursor returned by search_problems",
    )) {
      val result = rpc("tools/call", mapOf("name" to "search_problems", "arguments" to arguments))
      assertTrue(result["isError"].asBoolean(), result.toString())
      assertTrue(result["content"][0]["text"].asString().contains(message))
    }
  }

  @Test
  fun `all MCP operations require a valid token and do not create browser sessions`() {
    for (authorization in listOf(null, "Bearer incorrect", "Basic $TEST_TOKEN")) {
      val response = sendRpc("tools/list", authorization = authorization)
      assertEquals(401, response.statusCode(), response.body())
      assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElse(null))
    }

    val response = sendRpc("tools/list")
    assertEquals(200, response.statusCode())
    assertTrue(response.headers().allValues("Set-Cookie").isEmpty())
  }

  @Test
  fun `administrator revocation takes effect on the next MCP request`() {
    entClient.users.update(1) { role = UserRole.USER }.save(fixtures).getOrThrow()

    try {
      assertEquals(403, sendRpc("tools/list").statusCode())
      assertEquals(403, sendRpc("tools/call", mapOf("name" to "get_problem", "arguments" to mapOf("slug" to "mcp-alpha"))).statusCode())
    } finally {
      entClient.users.update(1) { role = UserRole.ADMIN }.save(fixtures).getOrThrow()
    }

    assertEquals(200, sendRpc("tools/list").statusCode())
  }

  @Test
  fun `browser sessions cannot replace tokens and bearer tokens do not bypass GraphQL CSRF`() {
    val principal = UserDetailsImpl(User("mcp-admin@example.com", "unused", UserRole.ADMIN, 1))
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
    val session = MockHttpSession()
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context)

    val mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<DefaultMockMvcBuilder>(securityFilter)
      .build()
    val response = mvc.perform(post("/mcp").session(session).contentType("application/json").content("{}"))
      .andReturn().response
    assertEquals(401, response.status)

    val graphql = mvc.perform(post("/graphql")
      .header("Authorization", "Bearer $TEST_TOKEN")
      .contentType("application/json")
      .content("{\"query\":\"{ languages { key } }\"}"))
      .andReturn().response
    assertEquals(403, graphql.status)
  }

  @Test
  fun `untrusted origins and hosts are rejected even with a valid token`() {
    assertEquals(403, sendRpc("tools/list", origin = "https://untrusted.example").statusCode())
    assertEquals(200, sendRpc("tools/list", origin = "http://localhost:$port").statusCode())

    val mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<DefaultMockMvcBuilder>(securityFilter)
      .build()
    val response = mvc.perform(post("/mcp")
      .header("Authorization", "Bearer $TEST_TOKEN")
      .header("Host", "untrusted.example")
      .accept("application/json", "text/event-stream")
      .contentType("application/json")
      .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
      .andReturn().response
    assertEquals(421, response.status)
  }

  private fun callTool(name: String, arguments: Map<String, Any> = emptyMap()): JsonNode {
    val result = rpc("tools/call", mapOf("name" to name, "arguments" to arguments))
    assertFalse(result["isError"].asBoolean(), result.toString())
    return result["structuredContent"]
  }

  private fun rpc(method: String, params: Map<String, Any> = emptyMap()): JsonNode {
    val response = sendRpc(method, params)
    assertEquals(200, response.statusCode(), response.body())
    val body = mapper.readTree(response.body())
    assertFalse(body.has("error"), body.toString())
    return body["result"]
  }

  private fun sendRpc(
    method: String,
    params: Map<String, Any> = emptyMap(),
    authorization: String? = "Bearer $TEST_TOKEN",
    origin: String? = null,
  ): HttpResponse<String> {
    val request = HttpRequest.newBuilder(URI("http://localhost:$port/mcp"))
      .timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json, text/event-stream")
      .header("MCP-Protocol-Version", "2025-11-25")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(mapOf(
        "jsonrpc" to "2.0", "id" to 1, "method" to method, "params" to params,
      ))))

    authorization?.let { request.header("Authorization", it) }
    origin?.let { request.header("Origin", it) }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString())
  }

  companion object {
    const val TEST_TOKEN = "mcp-integration-test-token-never-use-outside-tests"

    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

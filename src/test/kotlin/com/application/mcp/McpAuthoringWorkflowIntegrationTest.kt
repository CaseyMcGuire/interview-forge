package com.application.mcp

import com.application.ent.EntClient
import com.application.execution.DockerExecutionService
import com.application.execution.DockerExecutionServiceTest
import com.application.schema.UserRole
import entkt.runtime.privacy.ViewerContext
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpRequest
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = [
    "mcp.api-token=${McpServerIntegrationTest.TEST_TOKEN}", "mcp.user-id=1",
    "execution.runtimes.kotlin=" + DockerExecutionServiceTest.IMAGE,
    "execution.worker-enabled=true",
  ],
)
class McpAuthoringWorkflowIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed the MCP administrator in an isolated database")
  private lateinit var client: McpSyncClient

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var docker: DockerExecutionService
  @Autowired @Qualifier("mcpServerJsonMapper") lateinit var mapper: JsonMapper
  @LocalServerPort private var port = 0

  @BeforeAll
  fun createAdministrator() {
    val admin = entClient.users.create {
      email = "mcp-workflow@example.com"
      hashedPassword = "unused"
      role = UserRole.ADMIN
    }.saveAndLoad(fixtures).getOrThrow()
    assertEquals(1L, admin.id)
  }

  @AfterEach
  fun closeClient() {
    if (::client.isInitialized) {
      client.closeGracefully()
    }
  }

  @Test
  fun `an MCP client authors and verifies the documented example through the scheduled worker`() {
    assumeTrue(docker.isAvailable(DockerExecutionServiceTest.IMAGE), "Build the Kotlin execution image first")
    connectClient()

    val initialized = client.initialize()
    assertEquals("interview-forge", initialized.serverInfo().name())
    assertTrue(initialized.instructions().contains("enqueue_problem_submission"))
    assertEquals(11, client.listTools().tools().size)
    assertTrue(callTool("list_languages")["languages"].toList().any { it["key"].asString() == "kotlin" })
    assertEquals(0, callTool("search_problems", mapOf("search" to "Echo an integer"))["problems"].size())

    val example = mapper.readValue(
      Path.of("docs/examples/echo-integer.json").toFile(),
      object : TypeReference<Map<String, Any>>() {},
    )
    val slug = example.getValue("slug") as String
    assertEquals(slug, callTool("create_problem", example)["problem"]["slug"].asString())
    val target = mapOf("slug" to slug, "languageKey" to "kotlin")

    val generated = callTool("generate_expected_output", target + ("inputJson" to "9007199254740993"))
    assertEquals("9007199254740993", generated["expectedOutputJson"].asString())
    callTool("add_test_cases", mapOf(
      "slug" to slug,
      "testCases" to listOf(mapOf(
        "inputJson" to "9007199254740993",
        "expectedOutputJson" to generated["expectedOutputJson"].asString(),
      )),
    ))

    val problem = callTool("get_problem", mapOf("slug" to slug))["problem"]
    assertEquals(1, problem["publicExamples"].size())
    assertEquals(1, problem["testCases"].size())
    assertEquals("9007199254740993", problem["testCases"][0]["expectedOutputJson"].asString())

    val reference = enqueueAndAwaitSubmission(target)
    assertEquals("ACCEPTED", reference["verdict"].asString(), reference.toString())
    assertEquals(2, reference["passedCases"].asInt())
    assertEquals(2, reference["totalCases"].asInt())

    val incorrect = enqueueAndAwaitSubmission(target + (
      "sourceCode" to "class Solution { fun echo(value: Long): Long = 0L }"
    ))
    assertEquals("WRONG_ANSWER", incorrect["verdict"].asString(), incorrect.toString())
    assertEquals("1", incorrect["failedExample"]["expectedOutputJson"].asString())
    assertEquals("0", incorrect["failedExample"]["output"].asString())
    assertTrue(entClient.gradingJobs.query().all(fixtures).getOrThrow().isEmpty())
    Files.list(executionRoot).use { assertEquals(0L, it.count()) }
  }

  private fun connectClient() {
    val transport = HttpClientStreamableHttpTransport.builder("http://localhost:$port")
      .endpoint("/mcp")
      .jsonMapper(JacksonMcpJsonMapper(mapper))
      .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer ${McpServerIntegrationTest.TEST_TOKEN}"))
      .build()

    client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(180)).build()
  }

  private fun enqueueAndAwaitSubmission(arguments: Map<String, String>): JsonNode {
    val queued = callTool("enqueue_problem_submission", arguments)["problemSubmission"]
    val id = queued["id"].asString()
    lateinit var submission: JsonNode

    await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofSeconds(1)).untilAsserted {
      submission = callTool("get_problem_submission", mapOf("submissionId" to id))["problemSubmission"]
      assertEquals("FINISHED", submission["status"].asString())
    }

    return submission
  }

  private fun callTool(name: String, arguments: Map<String, Any> = emptyMap()): JsonNode {
    val result = client.callTool(CallToolRequest.builder(name).arguments(arguments).build())
    assertFalse(result.isError() == true, result.toString())
    return mapper.valueToTree(result.structuredContent())
  }

  companion object {
    private val executionRoot = Files.createTempDirectory("mcp-authoring-workflow-")

    @JvmStatic
    @DynamicPropertySource
    fun executionProperties(registry: DynamicPropertyRegistry) {
      registry.add("execution.workspace-directory") { executionRoot.toString() }
    }

    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

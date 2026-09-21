package com.application.mcp

import com.application.ent.EntClient
import com.application.execution.CodeExecutionResult
import com.application.execution.CodeExecutionService
import com.application.execution.CodeGrader
import com.application.execution.DockerExecutionService
import com.application.execution.DockerExecutionServiceTest
import com.application.execution.PreparedProgram
import com.application.execution.ProgramStatus
import com.application.execution.TestCaseExecutionResult
import com.application.execution.TestCaseInput
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import com.application.services.CodeExecutionSettingsService
import com.application.services.GradingJobService
import entkt.runtime.privacy.ViewerContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
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
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = [
    "mcp.api-token=${McpServerIntegrationTest.TEST_TOKEN}", "mcp.user-id=1",
    "execution.runtimes.kotlin=" + DockerExecutionServiceTest.IMAGE,
    "execution.worker-enabled=false",
  ],
)
@Import(ProblemExecutionToolsIntegrationTest.ExecutionConfiguration::class)
class ProblemExecutionToolsIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated MCP execution fixtures")
  private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
  private val arguments = mapOf("slug" to "mcp-execution", "languageKey" to "kotlin")
  private var problemId = 0L
  private var configurationId = 0L
  private var judgeId = 0L

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var mapper: ObjectMapper
  @Autowired lateinit var executor: TestExecutionService
  @Autowired lateinit var docker: DockerExecutionService
  @Autowired lateinit var gradingJobs: GradingJobService
  @Autowired lateinit var settings: CodeExecutionSettingsService
  @Autowired lateinit var grader: CodeGrader
  @LocalServerPort private var port = 0

  @BeforeAll
  fun createProblem() {
    val admin = entClient.users.create {
      email = "execution-admin@example.com"
      hashedPassword = "unused"
      role = UserRole.ADMIN
    }.saveAndLoad(fixtures).getOrThrow()
    assertEquals(1L, admin.id)

    problemId = entClient.problems.create {
      slug = "mcp-execution"
      title = "Echo JSON"
      statementMarkdown = "Return the supplied JSON."
      difficulty = ProblemDifficulty.EASY
      createdByUserId = admin.id
      publishedAt = Instant.now()
    }.saveAndLoad(fixtures).getOrThrow().id

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      problemId = this@ProblemExecutionToolsIntegrationTest.problemId
      languageId = language.id
      starterCode = "fun solve(input: String): String = TODO()"
    }.saveAndLoad(fixtures).getOrThrow().id

    judgeId = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      testDriverCode = "fun main() { print(solve(System.`in`.bufferedReader().readText())) }"
      referenceSolutionCode = REFERENCE_CODE
      timeLimitMs = 1500
      memoryLimitMb = 256
    }.saveAndLoad(fixtures).getOrThrow().id

    for ((position, visibility) in listOf(TestCaseVisibility.EXAMPLE, TestCaseVisibility.HIDDEN).withIndex()) {
      entClient.testCases.create {
        problemId = this@ProblemExecutionToolsIntegrationTest.problemId
        this.position = position
        this.visibility = visibility
        inputJson = JsonPrimitive(position + 1)
        expectedOutputJson = JsonPrimitive(position + 1)
      }.save(fixtures).getOrThrow()
    }
  }

  @BeforeEach
  fun resetExecution() {
    executor.available = true
    executor.result = null
    executor.delegate = null
    executor.programs.clear()
    entClient.gradingJobs.deleteMany(fixtures).getOrThrow()
    entClient.problemSubmissionFailures.deleteMany(fixtures).getOrThrow()
    entClient.problemSubmissions.deleteMany(fixtures).getOrThrow()
    entClient.judgeConfigurations.update(judgeId) { referenceSolutionCode = REFERENCE_CODE }.save(fixtures).getOrThrow()
  }

  @Test
  fun `generation executes stored reference code and preserves exact JSON without saving anything`() {
    val counts = storedCounts()
    for (input in listOf("[9007199254740993,1.0,null]", "null")) {
      val result = callTool("generate_expected_output", arguments + ("inputJson" to input))
      assertFalse(result["isError"].asBoolean(), result.toString())
      assertEquals(input, result["structuredContent"]["expectedOutputJson"].asString())
    }
    assertTrue(executor.programs.all { it.sourceFiles["Solution.kt"] == REFERENCE_CODE })
    assertEquals(counts, storedCounts())
  }

  @Test
  fun `generation reports invalid inputs unavailable execution and reference failures`() {
    for (input in listOf("broken", "1 2", "{\"x\":1,\"x\":2}")) {
      val result = callTool("generate_expected_output", arguments + ("inputJson" to input))
      assertTrue(result["isError"].asBoolean())
      assertEquals("inputJson", result["structuredContent"]["errors"][0]["field"].asString())
    }
    assertTrue(executor.programs.isEmpty())

    executor.available = false
    assertTrue(callTool("generate_expected_output", arguments + ("inputJson" to "null"))["isError"].asBoolean())
    executor.available = true
    executor.result = CodeExecutionResult.CompilationFailed
    val failed = callTool("generate_expected_output", arguments + ("inputJson" to "null"))
    assertTrue(failed["isError"].asBoolean())
    assertTrue(failed["structuredContent"]["errors"][0]["message"].asString().contains("compile"))
  }

  @Test
  fun `reference verification queues a snapshot and polling observes the completed grade`() {
    val id = enqueue()
    val queued = poll(id)
    assertEquals("QUEUED", queued["status"].asString())
    assertEquals("PENDING", queued["verdict"].asString())
    assertEquals(2, queued["totalCases"].asInt())
    assertTrue(executor.programs.isEmpty())
    assertEquals(REFERENCE_CODE, entClient.gradingJobs.query().all(fixtures).getOrThrow().single().sourceCode)

    gradeNextJob()

    val finished = poll(id)
    assertEquals("FINISHED", finished["status"].asString())
    assertEquals("ACCEPTED", finished["verdict"].asString())
    assertEquals(2, finished["passedCases"].asInt())
    assertFalse(finished["startedAt"].isNull)
    assertFalse(finished["finishedAt"].isNull)
    assertTrue(entClient.gradingJobs.query().all(fixtures).getOrThrow().isEmpty())
  }

  @Test
  fun `candidate code is queued as supplied and compilation failures are pollable`() {
    val id = enqueue("invalid candidate code")
    executor.result = CodeExecutionResult.CompilationFailed
    gradeNextJob()

    assertEquals("invalid candidate code", executor.programs.single().sourceFiles["Solution.kt"])
    val result = poll(id)
    assertEquals("FINISHED", result["status"].asString())
    assertEquals("COMPILE_ERROR", result["verdict"].asString())
    assertTrue(result["publicErrorMessage"].asString().contains("Compilation failed"))
  }

  @Test
  fun `polling returns failed public examples while keeping hidden failure details private`() {
    for (failedInput in 1..2) {
      val id = enqueue("candidate")
      executor.result = CodeExecutionResult.Completed(
        ProgramStatus.SUCCEEDED,
        (1..2).map { input ->
          val output = if (input == failedInput) 0 else input
          TestCaseExecutionResult(
            inputJson = JsonPrimitive(input),
            status = ProgramStatus.SUCCEEDED,
            outputJson = JsonPrimitive(output),
            stdout = output.toString(),
          )
        },
      )
      gradeNextJob()

      val submission = poll(id)
      assertEquals("WRONG_ANSWER", submission["verdict"].asString())
      assertEquals(1, submission["passedCases"].asInt())

      if (failedInput == 1) {
        assertEquals("1", submission["failedExample"]["inputJson"].asString())
        assertEquals("1", submission["failedExample"]["expectedOutputJson"].asString())
        assertEquals("0", submission["failedExample"]["output"].asString())
      } else {
        assertTrue(submission["failedExample"].isNull)
      }
    }
  }

  @Test
  fun `queue limits invalid source and missing references return errors without extra jobs`() {
    val invalid = callTool("enqueue_problem_submission", arguments + ("sourceCode" to " "))
    assertTrue(invalid["isError"].asBoolean())
    assertEquals("sourceCode", invalid["structuredContent"]["errors"][0]["field"].asString())

    entClient.judgeConfigurations.update(judgeId) { referenceSolutionCode = null }.save(fixtures).getOrThrow()
    assertTrue(callTool("enqueue_problem_submission", arguments)["isError"].asBoolean())

    enqueue("candidate one")
    enqueue("candidate two")
    val busy = callTool("enqueue_problem_submission", arguments + ("sourceCode" to "candidate three"))
    assertTrue(busy["isError"].asBoolean())
    assertTrue(busy["structuredContent"]["errors"][0]["message"].asString().contains("capacity"))
    assertEquals(2, entClient.gradingJobs.query().all(fixtures).getOrThrow().size)
  }

  @Test
  fun `polling hides other users submissions and invalid identifiers`() {
    val otherUser = entClient.users.create {
      email = "other-owner@example.com"
      hashedPassword = "unused"
    }.saveAndLoad(fixtures).getOrThrow()
    val otherSubmission = entClient.problemSubmissions.create {
      userId = otherUser.id
      problemId = this@ProblemExecutionToolsIntegrationTest.problemId
      problemLanguageId = configurationId
      sourceCode = "private candidate"
      totalCases = 2
    }.saveAndLoad(fixtures).getOrThrow()

    for (id in listOf("bad-id", "0", Long.MAX_VALUE.toString(), otherSubmission.id.toString())) {
      assertTrue(poll(id).isNull)
    }

    for (invalid in listOf(arguments + ("slug" to "missing"), arguments + ("languageKey" to "missing"))) {
      assertTrue(callTool("enqueue_problem_submission", invalid)["isError"].asBoolean())
      assertTrue(callTool("generate_expected_output", invalid + ("inputJson" to "null"))["isError"].asBoolean())
    }
  }

  @Test
  fun `MCP reference generation and queued verification run through Docker`() {
    assumeTrue(docker.isAvailable(DockerExecutionServiceTest.IMAGE), "Build the Kotlin execution image first")
    executor.delegate = docker

    val generated = callTool("generate_expected_output", arguments + ("inputJson" to "[9007199254740993,1.0,null]"))
    assertFalse(generated["isError"].asBoolean(), generated.toString())
    assertEquals("[9007199254740993,1.0,null]", generated["structuredContent"]["expectedOutputJson"].asString())

    val id = enqueue()
    gradeNextJob()
    assertEquals("ACCEPTED", poll(id)["verdict"].asString())
    Files.list(executionRoot).use { assertEquals(0L, it.count()) }
  }

  private fun storedCounts() = listOf(
    entClient.testCases.query().all(fixtures).getOrThrow().size,
    entClient.problemSubmissions.query().all(fixtures).getOrThrow().size,
    entClient.gradingJobs.query().all(fixtures).getOrThrow().size,
  )

  private fun enqueue(sourceCode: String? = null): String {
    val input = if (sourceCode == null) arguments else arguments + ("sourceCode" to sourceCode)
    val result = callTool("enqueue_problem_submission", input)
    assertFalse(result["isError"].asBoolean(), result.toString())
    return result["structuredContent"]["problemSubmission"]["id"].asString()
  }

  private fun poll(id: String): JsonNode {
    val result = callTool("get_problem_submission", mapOf("submissionId" to id))
    assertFalse(result["isError"].asBoolean(), result.toString())
    return result["structuredContent"]["problemSubmission"]
  }

  private fun gradeNextJob() {
    val job = requireNotNull(gradingJobs.claimNextQueuedGradingJob())
    val configuration = settings.loadSubmittedCodeSettings(job.problemLanguageId, job.sourceCode)
    val result = grader.gradeCode(
      executionId = "mcp-verification-${job.id}",
      runtime = configuration.runtime,
      program = configuration.program,
      cases = job.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
      timeLimitMs = configuration.timeLimitMs,
      memoryLimitMb = configuration.memoryLimitMb,
    )
    gradingJobs.finishGradingJob(job.id, result)
  }

  private fun callTool(name: String, arguments: Map<String, String>): JsonNode {
    val request = HttpRequest.newBuilder(URI("http://localhost:$port/mcp"))
      .timeout(Duration.ofSeconds(90))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json, text/event-stream")
      .header("MCP-Protocol-Version", "2025-11-25")
      .header("Authorization", "Bearer ${McpServerIntegrationTest.TEST_TOKEN}")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(mapOf(
        "jsonrpc" to "2.0", "id" to 1, "method" to "tools/call",
        "params" to mapOf("name" to name, "arguments" to arguments),
      ))))
      .build()

    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    assertEquals(200, response.statusCode(), response.body())
    val body = mapper.readTree(response.body())
    assertFalse(body.has("error"), body.toString())
    return body["result"]
  }

  class TestExecutionService : CodeExecutionService {
    var available = true
    var result: CodeExecutionResult? = null
    var delegate: CodeExecutionService? = null
    val programs = mutableListOf<PreparedProgram>()

    override fun isAvailable(runtime: String) = available
    override fun cleanUpInterruptedExecutions() = Unit

    override fun executeCode(
      executionId: String,
      runtime: String,
      program: PreparedProgram,
      inputs: List<JsonElement>,
      timeLimitMs: Int,
      memoryLimitMb: Int,
    ): CodeExecutionResult {
      programs += program
      delegate?.let { return it.executeCode(executionId, runtime, program, inputs, timeLimitMs, memoryLimitMb) }
      result?.let { return it }

      return CodeExecutionResult.Completed(
        ProgramStatus.SUCCEEDED,
        inputs.map { TestCaseExecutionResult(it, ProgramStatus.SUCCEEDED, outputJson = it, stdout = it.toString()) },
      )
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  class ExecutionConfiguration {
    @Bean
    @Primary
    fun testExecutionService() = TestExecutionService()
  }

  companion object {
    private const val REFERENCE_CODE = "fun solve(input: String): String = input"
    private val executionRoot = Files.createTempDirectory("mcp-execution-")

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

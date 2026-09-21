package com.application.graphql

import com.application.ent.EntClient
import com.application.execution.CodeExecutionResult
import com.application.execution.CodeExecutionService
import com.application.execution.DockerExecutionService
import com.application.execution.DockerExecutionServiceTest
import com.application.execution.PreparedProgram
import com.application.execution.ProgramStatus
import com.application.execution.TestCaseExecutionResult
import com.application.schema.ProblemDifficulty
import com.application.schema.ProblemCheckerKind
import com.application.schema.UserRole
import entkt.runtime.privacy.ViewerContext
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import com.application.graphql.types.Problem as GraphqlProblem
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage

@Testcontainers
@SpringBootTest(properties = [
  "execution.runtimes.kotlin=" + DockerExecutionServiceTest.IMAGE,
  "execution.worker-enabled=false",
])
@Import(TestCaseExpectedOutputIntegrationTest.ExecutionConfiguration::class)
class TestCaseExpectedOutputIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS("Seed and inspect isolated expected-output fixtures")

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var context: WebApplicationContext
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var mapper: ObjectMapper
  @Autowired lateinit var globalIdUtil: GlobalIdUtil
  @Autowired lateinit var executor: TestExecutionService
  @Autowired lateinit var docker: DockerExecutionService
  @Autowired @Qualifier("springSecurityFilterChain") lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc
  private lateinit var session: MockHttpSession
  private var userId = 0L
  private var problemId = 0L
  private var configurationId = 0L
  private var judgeId = 0L
  private val publicConfigurationId get() = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configurationId)

  @BeforeEach
  fun setUp() {
    executor.reset()
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()
    val login = login(UserRole.ADMIN)
    userId = login.first
    session = login.second

    problemId = entClient.problems.create {
      slug = "expected-output-${UUID.randomUUID()}"
      title = "Expected output fixture"
      statementMarkdown = "Return the input"
      difficulty = ProblemDifficulty.EASY
      createdByUserId = userId
      publishedAt = Instant.now().minusSeconds(60)
    }.saveAndLoad(fixtures).getOrThrow().id

    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    configurationId = entClient.problemLanguages.create {
      this.problemId = this@TestCaseExpectedOutputIntegrationTest.problemId
      languageId = language.id
      starterCode = "starter code must not run"
    }.saveAndLoad(fixtures).getOrThrow().id

    judgeId = entClient.judgeConfigurations.create {
      problemLanguageId = configurationId
      referenceSolutionCode = "fun solve(input: String): String = input"
      testDriverCode = "fun main() { print(solve(System.`in`.bufferedReader().readText())) }"
      timeLimitMs = 1500
      memoryLimitMb = 256
    }.saveAndLoad(fixtures).getOrThrow().id
  }

  @Test
  fun `generation uses stored reference code and limits without saving tests or attempts`() {
    val before = storedExecutionCounts()
    val result = generate("  [9007199254740993, 1.0, null]  ")

    assertEquals("[9007199254740993,1.0,null]", expectedOutput(result))
    val execution = executor.calls.single()
    assertEquals(DockerExecutionServiceTest.IMAGE, execution.runtime)
    assertEquals("fun solve(input: String): String = input", execution.program.sourceFiles["Solution.kt"])
    assertTrue(execution.program.sourceFiles.getValue("TestDriver.kt").contains("print(solve("))
    assertEquals(1500, execution.timeLimitMs)
    assertEquals(256, execution.memoryLimitMb)
    assertEquals(before, storedExecutionCounts())

    generate()
    assertNotEquals(executor.calls[0].executionId, executor.calls[1].executionId)
  }

  @Test
  fun `anonymous ordinary and revoked admins cannot generate answers`() {
    val (_, ordinary) = login(UserRole.USER)
    for (viewer in listOf(null, ordinary)) {
      assertType("ProblemForbidden", generate(session = viewer))
    }

    entClient.users.update(userId) { role = UserRole.USER }.save(fixtures).getOrThrow()
    assertType("ProblemForbidden", generate(configuration = "bad!", input = "not json"))
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `invalid IDs and JSON are field errors before execution`() {
    for (id in listOf("bad!", globalIdUtil.toGlobalId(GraphqlProblem::class, problemId))) {
      assertInvalid(generate(configuration = id), "problemLanguageId")
    }

    for (input in listOf("", " ", "{broken", "word", "1 2", "NaN", "{\"a\":1,\"a\":2}",
      JsonPrimitive("x".repeat(19_999)).toString())) {
      assertInvalid(generate(input), "inputJson")
    }
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `JSON null and the compact input size boundary are accepted`() {
    assertEquals("null", expectedOutput(generate("null")))
    assertEquals(JsonNull, executor.calls.single().inputs.single())

    val boundary = JsonPrimitive("x".repeat(19_998)).toString()
    assertEquals(boundary, expectedOutput(generate("  $boundary  ")))
  }

  @Test
  fun `missing unpublished archived and disabled targets do not execute`() {
    assertType("ProblemNotFound", generate(configuration = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, Long.MAX_VALUE)))

    entClient.problems.update(problemId) { publishedAt = null }.save(fixtures).getOrThrow()
    assertType("ProblemNotFound", generate())

    entClient.problems.update(problemId) {
      publishedAt = Instant.now().minusSeconds(60)
      archivedAt = Instant.now()
    }.save(fixtures).getOrThrow()
    assertType("ProblemNotFound", generate())

    entClient.problems.update(problemId) { archivedAt = null }.save(fixtures).getOrThrow()
    val language = entClient.languages.indexes.key("kotlin").find(fixtures).getOrThrow()!!
    try {
      entClient.languages.update(language.id) { enabled = false }.save(fixtures).getOrThrow()
      assertType("ProblemNotFound", generate())
    } finally {
      entClient.languages.update(language.id) { enabled = true }.save(fixtures).getOrThrow()
    }
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `missing reference judge and unavailable runtime return execution unavailable`() {
    executor.available = false
    assertType("ExecutionUnavailable", generate())
    executor.available = true

    entClient.judgeConfigurations.update(judgeId) { referenceSolutionCode = null }.save(fixtures).getOrThrow()
    assertType("ExecutionUnavailable", generate())

    entClient.judgeConfigurations.deleteById(fixtures, judgeId).getOrThrow()
    assertType("ExecutionUnavailable", generate())
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `unsupported languages and checkers do not execute`() {
    val language = entClient.languages.create {
      key = "unsupported-${UUID.randomUUID()}"
      displayName = "Unsupported language"
      enabled = true
    }.saveAndLoad(fixtures).getOrThrow()
    val configuration = entClient.problemLanguages.create {
      this.problemId = this@TestCaseExpectedOutputIntegrationTest.problemId
      languageId = language.id
      starterCode = "starter"
    }.saveAndLoad(fixtures).getOrThrow()
    entClient.judgeConfigurations.create {
      problemLanguageId = configuration.id
      referenceSolutionCode = "reference"
      testDriverCode = "driver"
      timeLimitMs = 1000
      memoryLimitMb = 256
    }.save(fixtures).getOrThrow()

    assertType("ExecutionUnavailable", generate(configuration = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configuration.id)))

    entClient.problems.update(problemId) { checkerKind = ProblemCheckerKind.CUSTOM }.save(fixtures).getOrThrow()
    assertType("ExecutionUnavailable", generate())
    assertTrue(executor.calls.isEmpty())
  }

  @Test
  fun `compilation and process failures do not return an answer even when one was produced`() {
    executor.result = CodeExecutionResult.CompilationFailed
    assertType("ReferenceSolutionFailed", generate())

    for (status in ProgramStatus.entries.filter { it != ProgramStatus.SUCCEEDED }) {
      executor.result = CodeExecutionResult.Completed(status, listOf(successfulCase()))
      assertType("ReferenceSolutionFailed", generate())

      executor.result = CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, listOf(
        TestCaseExecutionResult(JsonNull, status, stderr = "private execution diagnostic"),
      ))
      val result = generate()
      assertType("ReferenceSolutionFailed", result)
      assertFalse(result.toString().contains("private execution diagnostic"))
    }
  }

  @Test
  fun `missing invalid and oversized answers are reference failures`() {
    val invalidCases = listOf(
      emptyList(),
      listOf(successfulCase(), successfulCase()),
      listOf(TestCaseExecutionResult(JsonNull, ProgramStatus.SUCCEEDED, stdout = "not JSON")),
      listOf(successfulCase(JsonPrimitive("x".repeat(19_999)))),
      listOf(successfulCase(JsonPrimitive("é".repeat(10_000)))),
    )
    for (cases in invalidCases) {
      executor.result = CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, cases)
      assertType("ReferenceSolutionFailed", generate())
    }
  }

  @Test
  fun `unexpected execution errors remain GraphQL errors`() {
    executor.failure = IllegalStateException("Unexpected execution failure")
    val response = request("null", publicConfigurationId, session)

    assertTrue(response.has("errors"), response.toString())
    assertFalse(response.toString().contains("ReferenceSolutionFailed"))
    assertEquals("generateTestCaseExpectedOutput", response["errors"][0]["path"][0].asString())
  }

  @Test
  fun `the mutation compiles and runs the stored reference solution in Docker`() {
    assumeTrue(docker.isAvailable(DockerExecutionServiceTest.IMAGE), "Build the Kotlin execution image first")
    executor.delegate = docker
    val before = storedExecutionCounts()

    assertEquals("[9007199254740993,1.0,null]", expectedOutput(generate("[9007199254740993,1.0,null]")))
    assertEquals(before, storedExecutionCounts())
    Files.list(executionRoot).use { assertEquals(0, it.count()) }
  }

  private fun successfulCase(output: JsonElement = JsonNull) =
    TestCaseExecutionResult(JsonNull, ProgramStatus.SUCCEEDED, outputJson = output)

  private fun storedExecutionCounts() = listOf(
    entClient.testCases.query().all(fixtures).getOrThrow().size,
    entClient.problemSubmissions.query().all(fixtures).getOrThrow().size,
    entClient.customInputSubmissions.query().all(fixtures).getOrThrow().size,
    entClient.customTestCases.query().all(fixtures).getOrThrow().size,
    entClient.gradingJobs.query().all(fixtures).getOrThrow().size,
  )

  private fun assertType(type: String, result: JsonNode) {
    assertEquals(type, result["__typename"].asString(), result.toString())
  }

  private fun expectedOutput(result: JsonNode): String {
    assertType("GenerateTestCaseExpectedOutputSuccess", result)
    return result["expectedOutputJson"].asString()
  }

  private fun assertInvalid(result: JsonNode, field: String) {
    assertType("ProblemValidationFailure", result)
    assertEquals(field, result["fieldErrors"][0]["field"].asString())
  }

  private fun generate(
    input: String = "null",
    configuration: String = publicConfigurationId,
    session: MockHttpSession? = this.session,
  ): JsonNode {
    val response = request(input, configuration, session)
    assertFalse(response.has("errors"), response.toString())
    return response["data"]["generateTestCaseExpectedOutput"]
  }

  private fun request(input: String, configuration: String, session: MockHttpSession?): JsonNode {
    val request = post("/graphql")
      .contentType("application/json")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .content(mapper.writeValueAsString(mapOf(
        "query" to MUTATION,
        "variables" to mapOf("input" to mapOf("problemLanguageId" to configuration, "inputJson" to input)),
      )))
    session?.let { request.session(it) }

    var result = mvc.perform(request).andReturn()
    if (result.request.isAsyncStarted) {
      result.getAsyncResult(90_000)
      result = mvc.perform(asyncDispatch(result)).andReturn()
    }

    assertEquals(200, result.response.status, result.response.contentAsString)
    return mapper.readTree(result.response.contentAsString)
  }

  private fun login(role: UserRole): Pair<Long, MockHttpSession> {
    val email = "${UUID.randomUUID()}@example.com"
    val user = entClient.users.create {
      this.email = email
      hashedPassword = passwordEncoder.encode("test-password")
      this.role = role
    }.saveAndLoad(fixtures).getOrThrow()

    val result = mvc.perform(post("/login")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .param("username", email)
      .param("password", "test-password"),
    ).andReturn()

    assertEquals("/", result.response.redirectedUrl)
    return user.id to (result.request.session as MockHttpSession)
  }

  data class ExecutionCall(
    val executionId: String,
    val runtime: String,
    val program: PreparedProgram,
    val inputs: List<JsonElement>,
    val timeLimitMs: Int,
    val memoryLimitMb: Int,
  )

  class TestExecutionService : CodeExecutionService {
    var available = true
    var result: CodeExecutionResult? = null
    var failure: RuntimeException? = null
    var delegate: CodeExecutionService? = null
    val calls = mutableListOf<ExecutionCall>()

    fun reset() {
      available = true
      result = null
      failure = null
      delegate = null
      calls.clear()
    }

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
      calls += ExecutionCall(executionId, runtime, program, inputs, timeLimitMs, memoryLimitMb)
      failure?.let { throw it }
      delegate?.let { return it.executeCode(executionId, runtime, program, inputs, timeLimitMs, memoryLimitMb) }

      return result ?: CodeExecutionResult.Completed(ProgramStatus.SUCCEEDED, inputs.map {
        TestCaseExecutionResult(it, ProgramStatus.SUCCEEDED, outputJson = it)
      })
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  class ExecutionConfiguration {
    @Bean
    @Primary
    fun executionService() = TestExecutionService()
  }

  companion object {
    private val executionRoot = Files.createTempDirectory("expected-output-integration-")

    @DynamicPropertySource
    @JvmStatic
    fun executionProperties(registry: DynamicPropertyRegistry) {
      registry.add("execution.workspace-directory") { executionRoot.toString() }
    }

    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))

    private val MUTATION = """
      mutation GenerateExpectedOutput(${'$'}input: GenerateTestCaseExpectedOutputInput!) {
        generateTestCaseExpectedOutput(input: ${'$'}input) {
          __typename
          ... on GenerateTestCaseExpectedOutputSuccess { expectedOutputJson }
          ... on ProblemValidationFailure { message fieldErrors { field message } }
          ... on ProblemNotFound { message }
          ... on ProblemForbidden { message }
          ... on ExecutionUnavailable { message }
          ... on ReferenceSolutionFailed { message }
        }
      }
    """.trimIndent()
  }
}

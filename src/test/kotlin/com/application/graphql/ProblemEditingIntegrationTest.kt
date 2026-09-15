package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.TestCase
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntMutationPrivacyDeniedException
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
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
import java.time.Instant
import java.util.UUID
import com.application.graphql.types.Problem as GraphqlProblem
import com.application.graphql.types.ProblemExample as GraphqlProblemExample
import com.application.graphql.types.ProblemLanguage as GraphqlProblemLanguage

@Testcontainers
@SpringBootTest
class ProblemEditingIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect problem-editing fixtures in an isolated test database"
  )

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var applicationContext: WebApplicationContext
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var objectMapper: ObjectMapper
  @Autowired lateinit var globalIdUtil: GlobalIdUtil
  @Autowired @Qualifier("springSecurityFilterChain") lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc
  private var adminId: Long = 0
  private lateinit var adminSession: MockHttpSession
  private lateinit var problem: Problem
  private lateinit var configuration: ProblemLanguage
  private lateinit var otherConfiguration: ProblemLanguage
  private lateinit var example: TestCase
  private lateinit var otherExample: TestCase
  private lateinit var hidden: TestCase

  private val problemId get() = globalIdUtil.toGlobalId(GraphqlProblem::class, problem.id)
  private val configurationId get() = globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, configuration.id)
  private val exampleId get() = globalIdUtil.toGlobalId(GraphqlProblemExample::class, example.id)

  @BeforeEach
  fun setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()

    val login = login(UserRole.ADMIN)
    adminId = login.first
    adminSession = login.second
    problem = entClient.problems.create {
      slug = "edit-${UUID.randomUUID()}"
      title = "Two Sum"
      statementMarkdown = "Find two distinct indices."
      difficulty = ProblemDifficulty.EASY
      createdByUserId = adminId
      publishedAt = Instant.now()
    }.saveAndLoad(fixtureContext).getOrThrow()

    configuration = createConfiguration(createLanguage())
    otherConfiguration = createConfiguration(createLanguage())
    example = createExample(2)
    otherExample = createExample(8)
    hidden = createExample(1, TestCaseVisibility.HIDDEN)
  }

  @Test
  fun `editing problem properties preserves omitted content and all child records`() {
    val result = mutate("updateProblem", problemId, mapOf("title" to "  Find a Pair  "))
    assertEquals("UpdateProblemSuccess", result["__typename"].asString())
    assertEquals(problemId, result["problem"]["id"].asString())
    assertEquals("Find a Pair", result["problem"]["title"].asString())
    assertEquals(problem.statementMarkdown, result["problem"]["statementMarkdown"].asString())
    assertEquals("EASY", result["problem"]["difficulty"].asString())
    assertEquals(problem.slug, result["problem"]["slug"].asString())
    assertEquals(2, result["problem"]["languageConfigurations"].size())
    assertEquals(2, result["problem"]["examples"].size())

    val updated = mutate("updateProblem", problemId, mapOf(
      "statementMarkdown" to "A revised statement.",
      "difficulty" to "HARD",
    ))
    assertEquals("Find a Pair", updated["problem"]["title"].asString())
    assertEquals("A revised statement.", updated["problem"]["statementMarkdown"].asString())
    assertEquals("HARD", updated["problem"]["difficulty"].asString())
    assertEquals(configuration.starterCode, storedConfiguration(configuration.id).starterCode)
    assertEquals(otherConfiguration.starterCode, storedConfiguration(otherConfiguration.id).starterCode)
    assertExampleUnchanged(example)
    assertExampleUnchanged(otherExample)
    assertExampleUnchanged(hidden)
  }

  @Test
  fun `editing a stencil returns its language and preserves its assignments`() {
    val result = mutate("updateProblemLanguage", configurationId, mapOf("starterCode" to "updated source"))
    assertEquals("UpdateProblemLanguageSuccess", result["__typename"].asString())
    assertEquals(configurationId, result["problemLanguage"]["id"].asString())
    assertEquals("updated source", result["problemLanguage"]["starterCode"].asString())
    assertEquals(
      globalIdUtil.toGlobalId(com.application.graphql.types.Language::class, configuration.languageId),
      result["problemLanguage"]["language"]["id"].asString(),
    )
    val stored = storedConfiguration(configuration.id)
    assertEquals(configuration.problemId, stored.problemId)
    assertEquals(configuration.languageId, stored.languageId)
    assertEquals(otherConfiguration.starterCode, storedConfiguration(otherConfiguration.id).starterCode)
    assertEquals(problem.updatedAt, storedProblem().updatedAt)
    assertExampleUnchanged(example)
  }

  @Test
  fun `example updates distinguish omitted explanations explicit clears and JSON null values`() {
    val changedInput = "{\"nums\":[1,3],\"target\":4}"
    val updated = mutate("updateProblemExample", exampleId, mapOf("inputJson" to changedInput))
    assertEquals("UpdateProblemExampleSuccess", updated["__typename"].asString())
    assertEquals(example.explanationMarkdown, updated["example"]["explanationMarkdown"].asString())
    assertEquals(example.expectedOutputJson.toString(), updated["example"]["expectedOutputJson"].asString())

    val cleared = mutate("updateProblemExample", exampleId, mapOf("explanationMarkdown" to null))
    assertEquals("UpdateProblemExampleSuccess", cleared["__typename"].asString())
    assertTrue(cleared["example"]["explanationMarkdown"].isNull)
    assertEquals(Json.parseToJsonElement(changedInput), storedExample(example.id).inputJson)

    val jsonNull = mutate("updateProblemExample", exampleId, mapOf(
      "inputJson" to "null",
      "expectedOutputJson" to "null",
      "explanationMarkdown" to "The result is null.",
    ))
    assertEquals("UpdateProblemExampleSuccess", jsonNull["__typename"].asString())
    val stored = storedExample(example.id)
    assertEquals(JsonNull, stored.inputJson)
    assertEquals(JsonNull, stored.expectedOutputJson)
    assertEquals("The result is null.", stored.explanationMarkdown)
    assertEquals(example.position, stored.position)
    assertEquals(example.visibility, stored.visibility)
    assertEquals(example.problemId, stored.problemId)
    assertExampleUnchanged(otherExample)
    assertExampleUnchanged(hidden)
  }

  @Test
  fun `ID-only updates are no-ops including modification timestamps`() {
    assertEquals("UpdateProblemSuccess", mutate("updateProblem", problemId)["__typename"].asString())
    assertEquals("UpdateProblemLanguageSuccess", mutate("updateProblemLanguage", configurationId)["__typename"].asString())
    assertEquals("UpdateProblemExampleSuccess", mutate("updateProblemExample", exampleId)["__typename"].asString())
    assertAllUnchanged()
  }

  @Test
  fun `explicit null for required content is a no-op including modification timestamps`() {
    for (field in listOf("title", "statementMarkdown", "difficulty")) {
      val result = mutate("updateProblem", problemId, mapOf(field to null))
      assertEquals("UpdateProblemSuccess", result["__typename"].asString(), field)
    }
    val configuration = mutate("updateProblemLanguage", configurationId, mapOf("starterCode" to null))
    assertEquals("UpdateProblemLanguageSuccess", configuration["__typename"].asString())
    for (field in listOf("inputJson", "expectedOutputJson")) {
      val result = mutate("updateProblemExample", exampleId, mapOf(field to null))
      assertEquals("UpdateProblemExampleSuccess", result["__typename"].asString(), field)
    }
    assertAllUnchanged()
  }

  @Test
  fun `null fields leave values unchanged while other supplied fields are updated`() {
    val result = mutate("updateProblem", problemId, mapOf(
      "title" to "A revised title",
      "statementMarkdown" to null,
      "difficulty" to null,
    ))
    assertEquals("UpdateProblemSuccess", result["__typename"].asString())
    val stored = storedProblem()
    assertEquals("A revised title", stored.title)
    assertEquals(problem.statementMarkdown, stored.statementMarkdown)
    assertEquals(problem.difficulty, stored.difficulty)

    val exampleResult = mutate("updateProblemExample", exampleId, mapOf(
      "inputJson" to null,
      "expectedOutputJson" to "[1,0]",
      "explanationMarkdown" to null,
    ))
    assertEquals("UpdateProblemExampleSuccess", exampleResult["__typename"].asString())
    val storedExample = storedExample(example.id)
    assertEquals(example.inputJson, storedExample.inputJson)
    assertEquals(Json.parseToJsonElement("[1,0]"), storedExample.expectedOutputJson)
    assertNull(storedExample.explanationMarkdown)
  }

  @Test
  fun `invalid content reports field errors and rejects the whole mutation`() {
    assertValidation(mutate("updateProblem", problemId, mapOf("title" to " ")), "title")
    assertValidation(mutate("updateProblem", problemId, mapOf(
      "title" to "Must not be saved",
      "statementMarkdown" to " ",
    )), "statementMarkdown")
    assertValidation(mutate("updateProblemLanguage", configurationId, mapOf("starterCode" to " ")), "starterCode")
    assertValidation(mutate("updateProblemExample", exampleId, mapOf("inputJson" to "{broken")), "inputJson")
    assertValidation(mutate("updateProblemExample", exampleId, mapOf(
      "inputJson" to "[99]",
      "expectedOutputJson" to "{broken",
    )), "expectedOutputJson")

    val oversizedJson = JsonPrimitive("x".repeat(20_000)).toString()
    assertValidation(mutate("updateProblemExample", exampleId, mapOf("inputJson" to oversizedJson)), "inputJson")
    assertValidation(mutate("updateProblemExample", exampleId, mapOf(
      "inputJson" to "[99]",
      "expectedOutputJson" to oversizedJson,
    )), "expectedOutputJson")

    assertValidation(mutate("updateProblemExample", exampleId, mapOf(
      "inputJson" to "[99]",
      "explanationMarkdown" to "x".repeat(10_001),
    )), "explanationMarkdown")
    assertAllUnchanged()
  }

  @Test
  fun `malformed and wrong-type IDs are validation failures while missing targets are not found`() {
    for ((operation, id) in targets()) {
      assertValidation(mutate(operation, "not!!base64"), "id")
      val wrongType = if (operation == "updateProblem") {
        exampleId
      } else {
        problemId
      }
      assertValidation(mutate(operation, wrongType), "id")

      val missing = when (operation) {
        "updateProblem" -> globalIdUtil.toGlobalId(GraphqlProblem::class, Long.MAX_VALUE)
        "updateProblemLanguage" -> globalIdUtil.toGlobalId(GraphqlProblemLanguage::class, Long.MAX_VALUE)
        else -> globalIdUtil.toGlobalId(GraphqlProblemExample::class, Long.MAX_VALUE)
      }
      assertEquals("ProblemNotFound", mutate(operation, missing)["__typename"].asString(), id)
    }
    assertAllUnchanged()
  }

  @Test
  fun `unpublished future and archived problems cannot be edited through any mutation`() {
    val now = Instant.now()
    val availability = listOf(
      null to null,
      now.plusSeconds(3600) to null,
      now to now,
    )
    for ((publishedAt, archivedAt) in availability) {
      entClient.problems.update(problem.id) {
        this.publishedAt = publishedAt
        this.archivedAt = archivedAt
      }.save(fixtureContext).getOrThrow()

      for ((operation, id) in targets()) {
        assertEquals("ProblemNotFound", mutate(operation, id)["__typename"].asString(), operation)
      }

      val context = adminSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext
      SecurityContextHolder.setContext(context)
      try {
        val viewer = ViewerContext(Viewer.User(adminId))
        assertThrows(EntMutationPrivacyDeniedException::class.java) {
          entClient.problems.update(problem.id) { title = "Must not be saved" }.save(viewer).getOrThrow()
        }
        assertThrows(EntMutationPrivacyDeniedException::class.java) {
          entClient.problems.update(problem.id) {}.save(viewer).getOrThrow()
        }
      } finally {
        SecurityContextHolder.clearContext()
      }
    }
    assertEquals(problem.title, storedProblem().title)
    assertEquals(configuration.starterCode, storedConfiguration(configuration.id).starterCode)
    assertExampleUnchanged(example)
  }

  @Test
  fun `disabled language configurations and hidden test cases are unavailable`() {
    entClient.languages.update(configuration.languageId) { enabled = false }.save(fixtureContext).getOrThrow()
    val result = mutate("updateProblemLanguage", configurationId, mapOf("starterCode" to "changed"))
    assertEquals("ProblemNotFound", result["__typename"].asString())

    val hiddenId = globalIdUtil.toGlobalId(GraphqlProblemExample::class, hidden.id)
    for (operation in listOf("updateProblemExample", "deleteProblemExample")) {
      assertEquals("ProblemNotFound", mutate(operation, hiddenId)["__typename"].asString())
    }
    assertAllUnchanged()
  }

  @Test
  fun `deletion removes only the requested example and preserves remaining positions`() {
    val result = mutate("deleteProblemExample", exampleId)
    assertEquals("DeleteProblemExampleSuccess", result["__typename"].asString())
    assertEquals(exampleId, result["deletedExampleId"].asString())
    assertEquals(problemId, result["problem"]["id"].asString())
    assertEquals(1, result["problem"]["examples"].size())
    assertEquals(otherExample.position, result["problem"]["examples"][0]["position"].asInt())
    assertNull(entClient.testCases.findById(fixtureContext, example.id).getOrThrow())
    assertExampleUnchanged(otherExample)
    assertExampleUnchanged(hidden)
    assertEquals(configuration.starterCode, storedConfiguration(configuration.id).starterCode)
    assertEquals("ProblemNotFound", mutate("deleteProblemExample", exampleId)["__typename"].asString())

    val otherId = globalIdUtil.toGlobalId(GraphqlProblemExample::class, otherExample.id)
    val last = mutate("deleteProblemExample", otherId)
    assertEquals("DeleteProblemExampleSuccess", last["__typename"].asString())
    assertEquals(0, last["problem"]["examples"].size())
    assertExampleUnchanged(hidden)
  }

  @Test
  fun `anonymous and ordinary viewers receive forbidden results without writes`() {
    val (_, ordinarySession) = login(UserRole.USER)
    for (session in listOf(null, ordinarySession)) {
      for ((operation, id) in targets()) {
        assertEquals("ProblemForbidden", mutate(operation, id, session = session)["__typename"].asString())
      }
    }
    assertAllUnchanged()
  }

  @Test
  fun `revocation and promotion apply to all mutations in the existing session`() {
    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
    for ((operation, id) in targets()) {
      assertEquals("ProblemForbidden", mutate(operation, id)["__typename"].asString())
    }
    assertAllUnchanged()

    entClient.users.update(adminId) { role = UserRole.ADMIN }.save(fixtureContext).getOrThrow()
    assertEquals("UpdateProblemSuccess", mutate("updateProblem", problemId, mapOf("title" to "Edited"))["__typename"].asString())
    assertEquals("UpdateProblemLanguageSuccess", mutate("updateProblemLanguage", configurationId, mapOf("starterCode" to "edited"))["__typename"].asString())
    assertEquals("UpdateProblemExampleSuccess", mutate("updateProblemExample", exampleId, mapOf("inputJson" to "[]"))["__typename"].asString())
    assertEquals("DeleteProblemExampleSuccess", mutate("deleteProblemExample", exampleId)["__typename"].asString())
  }

  @Test
  fun `Ent update and delete policies reject mismatched viewers and revoked admins`() {
    val context = adminSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext
    SecurityContextHolder.setContext(context)
    try {
      assertWritesDenied(ViewerContext(Viewer.Anonymous))
      assertWritesDenied(ViewerContext(Viewer.User(adminId + 1000)))
      entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
      assertWritesDenied(ViewerContext(Viewer.User(adminId)))
    } finally {
      SecurityContextHolder.clearContext()
    }
    assertAllUnchanged()
  }

  private fun assertWritesDenied(context: ViewerContext) {
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.problems.update(problem.id) { title = "Denied" }.save(context).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.problemLanguages.update(configuration.id) { starterCode = "Denied" }.save(context).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.testCases.update(example.id) { explanationMarkdown = "Denied" }.save(context).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.testCases.deleteById(context, example.id).getOrThrow()
    }
  }

  private fun assertValidation(result: JsonNode, field: String) {
    assertEquals("ProblemValidationFailure", result["__typename"].asString(), result.toString())
    assertTrue(result["fieldErrors"].any { it["field"].asString() == field }, result.toString())
  }

  private fun assertAllUnchanged() {
    val stored = storedProblem()
    assertEquals(problem.title, stored.title)
    assertEquals(problem.statementMarkdown, stored.statementMarkdown)
    assertEquals(problem.difficulty, stored.difficulty)
    assertEquals(problem.updatedAt, stored.updatedAt)
    for (original in listOf(configuration, otherConfiguration)) {
      val current = storedConfiguration(original.id)
      assertEquals(original.starterCode, current.starterCode)
      assertEquals(original.updatedAt, current.updatedAt)
    }
    for (original in listOf(example, otherExample, hidden)) {
      assertExampleUnchanged(original)
    }
  }

  private fun assertExampleUnchanged(original: TestCase) {
    val stored = storedExample(original.id)
    assertEquals(original.inputJson, stored.inputJson)
    assertEquals(original.expectedOutputJson, stored.expectedOutputJson)
    assertEquals(original.explanationMarkdown, stored.explanationMarkdown)
    assertEquals(original.position, stored.position)
    assertEquals(original.visibility, stored.visibility)
    assertEquals(original.updatedAt, stored.updatedAt)
  }

  private fun storedProblem() = entClient.problems.findById(fixtureContext, problem.id).getOrThrow()!!
  private fun storedConfiguration(id: Long) = entClient.problemLanguages.findById(fixtureContext, id).getOrThrow()!!
  private fun storedExample(id: Long) = entClient.testCases.findById(fixtureContext, id).getOrThrow()!!

  private fun targets() = listOf(
    "updateProblem" to problemId,
    "updateProblemLanguage" to configurationId,
    "updateProblemExample" to exampleId,
    "deleteProblemExample" to exampleId,
  )

  private fun createLanguage(): Language = entClient.languages.create {
    key = "edit-${UUID.randomUUID()}"
    displayName = "Test language"
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createConfiguration(language: Language): ProblemLanguage = entClient.problemLanguages.create {
    problemId = problem.id
    languageId = language.id
    starterCode = "initial source"
  }.saveAndLoad(fixtureContext).getOrThrow()

  private fun createExample(position: Int, visibility: TestCaseVisibility = TestCaseVisibility.EXAMPLE): TestCase =
    entClient.testCases.create {
      problemId = problem.id
      this.position = position
      this.visibility = visibility
      inputJson = Json.parseToJsonElement("[2,7]")
      expectedOutputJson = Json.parseToJsonElement("[0,1]")
      explanationMarkdown = "An existing explanation."
    }.saveAndLoad(fixtureContext).getOrThrow()

  private fun login(role: UserRole): Pair<Long, MockHttpSession> {
    val email = "${UUID.randomUUID()}@example.com"
    val user = entClient.users.create {
      this.email = email
      hashedPassword = passwordEncoder.encode("test-password")
      this.role = role
    }.saveAndLoad(fixtureContext).getOrThrow()
    val result = mvc.perform(post("/login")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .param("username", email)
      .param("password", "test-password")
    ).andReturn()
    assertEquals("/", result.response.redirectedUrl)
    return user.id to (result.request.session as MockHttpSession)
  }

  private fun mutate(
    operation: String,
    id: String,
    fields: Map<String, Any?> = emptyMap(),
    session: MockHttpSession? = adminSession,
  ): JsonNode {
    val inputType = operation.replaceFirstChar { it.uppercase() } + "Input"
    val successType = operation.replaceFirstChar { it.uppercase() } + "Success"
    val successFields = when (operation) {
      "updateProblem" -> """
        problem {
          id slug title statementMarkdown difficulty
          languageConfigurations { id starterCode }
          examples { id position }
        }
      """.trimIndent()
      "updateProblemLanguage" -> "problemLanguage { id starterCode language { id key } }"
      "updateProblemExample" -> "example { id inputJson expectedOutputJson explanationMarkdown }"
      "deleteProblemExample" -> "deletedExampleId problem { id examples { id position } }"
      else -> error("Unknown mutation: $operation")
    }
    val query = """
      mutation Edit(${'$'}input: $inputType!) {
        $operation(input: ${'$'}input) {
          __typename
          ... on $successType { $successFields }
          ... on ProblemFailure { message }
          ... on ProblemValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent()

    val request = post("/graphql")
      .contentType("application/json")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .content(objectMapper.writeValueAsString(mapOf(
        "query" to query,
        "variables" to mapOf("input" to (mapOf("id" to id) + fields)),
      )))
    session?.let { request.session(it) }
    var result = mvc.perform(request).andReturn()
    if (result.request.isAsyncStarted) {
      result = mvc.perform(asyncDispatch(result)).andReturn()
    }
    assertEquals(200, result.response.status, result.response.contentAsString)
    val response = objectMapper.readTree(result.response.contentAsString)
    assertFalse(response.has("errors"), response.toString())
    return response["data"][operation]
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

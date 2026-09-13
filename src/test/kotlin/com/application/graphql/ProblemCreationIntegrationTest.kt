package com.application.graphql

import com.application.ent.EntClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.TestCase
import com.application.db.models.UserDetailsImpl
import com.application.schema.ProblemDifficulty
import com.application.schema.TestCaseVisibility
import com.application.schema.UserRole
import jakarta.servlet.Filter
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.privacy.Viewer
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntValidationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.time.Instant

@Testcontainers
@SpringBootTest
class ProblemCreationIntegrationTest {
  private val fixtureContext = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect problem-creation fixtures in an isolated test database"
  )

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var applicationContext: WebApplicationContext
  @Autowired lateinit var passwordEncoder: PasswordEncoder
  @Autowired lateinit var objectMapper: ObjectMapper
  @Autowired @Qualifier("springSecurityFilterChain") lateinit var securityFilter: Filter

  private lateinit var mvc: MockMvc

  @BeforeEach
  fun setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(securityFilter)
      .build()
  }

  @Test
  fun `direct page and SPA decisions require an admin login`() {
    val (_, ordinary) = login(UserRole.USER)
    val (_, admin) = login(UserRole.ADMIN)

    for ((session, location) in listOf(null to "/login", ordinary to "/problems", admin to null)) {
      assertRouteAccess(session, location)
    }

    assertEquals(200, mvc.perform(get("/problem/two-sum")).andReturn().response.status)
  }

  @Test
  fun `promotion and revocation apply to routes and mutations within existing sessions`() {
    for (loginRole in listOf(UserRole.USER, UserRole.ADMIN)) {
      val (userId, session) = login(loginRole)
      val changedRole = if (loginRole == UserRole.USER) UserRole.ADMIN else UserRole.USER
      for (role in listOf(loginRole, changedRole)) {
        entClient.users.update(userId) { this.role = role }.save(fixtureContext).getOrThrow()
        assertRouteAccess(session, if (role == UserRole.ADMIN) null else "/problems")

        val input = input()
        val result = mutate(input, session)
        if (role == UserRole.ADMIN) {
          assertFalse(result.has("errors"), result.toString())
          assertEquals(userId, findProblem(input["slug"] as String)!!.createdByUserId)
        } else {
          assertTrue(result.has("errors"), result.toString())
          assertNull(findProblem(input["slug"] as String))
        }

        // The login snapshot stays unchanged; authorization must use the current database role.
        val principal = requireNotNull(sessionContext(session).authentication).principal as UserDetailsImpl
        assertEquals(loginRole, principal.user.role)
      }
    }
  }

  @Test
  fun `Ent creation rules read current roles and reject a mismatched viewer`() {
    val (userId, session) = login(UserRole.ADMIN)
    val context = ViewerContext(Viewer.User(userId))
    SecurityContextHolder.setContext(sessionContext(session))
    try {
      for (role in listOf(UserRole.ADMIN, UserRole.USER, UserRole.ADMIN)) {
        entClient.users.update(userId) { this.role = role }.save(fixtureContext).getOrThrow()
        val slug = "direct-${UUID.randomUUID()}"
        val create = {
          entClient.problems.create {
            this.slug = slug
            title = "Direct Ent permission check"
            statementMarkdown = "A test problem."
            difficulty = ProblemDifficulty.EASY
            createdByUserId = userId
            publishedAt = Instant.now()
          }
        }
        if (role == UserRole.ADMIN) {
          create().save(context).getOrThrow()
          assertNotNull(findProblem(slug))
          assertThrows(EntMutationPrivacyDeniedException::class.java) {
            create().save(ViewerContext(Viewer.User(userId + 1))).getOrThrow()
          }
        } else {
          assertThrows(EntMutationPrivacyDeniedException::class.java) {
            create().save(context).getOrThrow()
          }
          assertNull(findProblem(slug))
        }
      }
    } finally {
      SecurityContextHolder.clearContext()
    }
  }

  @Test
  fun `a deleted account loses access even with an existing admin session`() {
    val (userId, session) = login(UserRole.ADMIN)
    assertRouteAccess(session, null)
    entClient.users.deleteById(fixtureContext, userId).getOrThrow()

    assertRouteAccess(session, "/login")
    val input = input()
    val result = mutate(input, session)
    assertTrue(result.has("errors"), result.toString())
    assertNull(findProblem(input["slug"] as String))
  }

  @Test
  fun `anonymous and ordinary users cannot create problems through GraphQL`() {
    val (_, ordinary) = login(UserRole.USER)
    for (session in listOf(null, ordinary)) {
      val input = input()
      val result = mutate(input, session)
      assertTrue(result.has("errors"), result.toString())
      assertNull(findProblem(input["slug"] as String))
    }
  }

  @Test
  fun `admin creates shared content with multiple language configurations and an attributed author`() {
    val (authorId, session) = login(UserRole.ADMIN)
    val python = createLanguage("python-${UUID.randomUUID()}")
    val java = createLanguage("java-${UUID.randomUUID()}")
    val configurations = listOf(
      configuration(python.key, "def solve():\n    pass\n", "solution.py"),
      configuration(java.key, "class Solution {}", "Solution.java"),
    )
    val input = input() + ("languageConfigurations" to configurations)
    val result = mutate(input, session)
    assertFalse(result.has("errors"), result.toString())
    val created = result["data"]["createProblem"]
    assertEquals(input["slug"], created["slug"].asString())
    assertEquals("Create a pair", created["title"].asString())
    assertEquals(2, created["languageConfigurations"].size())
    for ((index, expected) in configurations.sortedBy { it["languageKey"] }.withIndex()) {
      val configuration = created["languageConfigurations"][index]
      assertEquals(expected["languageKey"], configuration["language"]["key"].asString())
      assertEquals(expected["solutionFilename"], configuration["solutionFilename"].asString())
      assertEquals(expected["starterCode"], configuration["starterCode"].asString())
    }
    val examples = created["examples"]
    assertEquals(2, examples.size())
    assertEquals(0, examples[0]["position"].asInt())
    assertEquals(1, examples[1]["position"].asInt())
    assertEquals("null", examples[1]["expectedOutputJson"].asString())
    assertEquals(authorId, findProblem(input["slug"] as String)!!.createdByUserId)

    val publicResult = graphql("query { problem(slug: \"${input["slug"]}\") { slug } }")
    assertEquals(input["slug"], publicResult["data"]["problem"]["slug"].asString())
  }

  @Test
  fun `Ent validators protect direct writes and updates`() {
    val (_, session) = login(UserRole.ADMIN)
    val input = input()
    assertFalse(mutate(input, session).has("errors"))
    val problem = findProblem(input["slug"] as String)!!
    val configuration = entClient.problemLanguages.query {
      where(ProblemLanguage.problemId eq problem.id)
    }.firstOrNull(fixtureContext).getOrThrow()!!
    val example = entClient.testCases.query {
      where(TestCase.problemId eq problem.id)
      where(TestCase.position eq 0)
    }.firstOrNull(fixtureContext).getOrThrow()!!

    fun expectValidation(field: String, write: () -> Unit) {
      val exception = assertThrows(EntValidationException::class.java) { write() }
      assertEquals(listOf(field), exception.violations.map { it.field })
    }

    expectValidation("title") {
      entClient.problems.update(problem.id) { title = " " }.save(fixtureContext).getOrThrow()
    }

    expectValidation("statementMarkdown") {
      entClient.problems.update(problem.id) { statementMarkdown = " " }.save(fixtureContext).getOrThrow()
    }

    expectValidation("starterCode") {
      entClient.problemLanguages.update(configuration.id) { starterCode = " " }.save(fixtureContext).getOrThrow()
    }

    expectValidation("solutionFilename") {
      entClient.problemLanguages.update(configuration.id) {
        solutionFilename = "../Solution.kt"
      }.save(fixtureContext).getOrThrow()
    }

    expectValidation("inputJson") {
      entClient.testCases.update(example.id) {
        inputJson = JsonPrimitive("x".repeat(20_000))
      }.save(fixtureContext).getOrThrow()
    }

    expectValidation("explanationMarkdown") {
      entClient.testCases.update(example.id) {
        explanationMarkdown = "x".repeat(10_001)
      }.save(fixtureContext).getOrThrow()
    }

    val disabledLanguage = createLanguage("disabled-${UUID.randomUUID()}", enabled = false)
    expectValidation("languageId") {
      entClient.problemLanguages.create {
        problemId = problem.id
        languageId = disabledLanguage.id
        starterCode = "class Solution {}"
        solutionFilename = "Solution.kt"
      }.save(fixtureContext).getOrThrow()
    }

    assertEquals(problem.title, findProblem(problem.slug)!!.title)
    assertEquals(
      configuration.starterCode,
      entClient.problemLanguages.findById(fixtureContext, configuration.id).getOrThrow()!!.starterCode,
    )
    assertEquals(example.inputJson, entClient.testCases.findById(fixtureContext, example.id).getOrThrow()!!.inputJson)
  }

  @Test
  fun `language choices include only enabled catalog entries in key order`() {
    val enabled = createLanguage("enabled-${UUID.randomUUID()}")
    val disabled = createLanguage("disabled-${UUID.randomUUID()}", enabled = false)
    val result = graphql("query { languages { id key displayName } }")
    assertFalse(result.has("errors"), result.toString())
    val languages = result["data"]["languages"].toList()
    val keys = languages.map { it["key"].asString() }
    assertEquals(keys.sorted(), keys)
    assertTrue(enabled.key in keys)
    assertFalse(disabled.key in keys)
    assertEquals(enabled.displayName, languages.single { it["key"].asString() == enabled.key }["displayName"].asString())
  }

  @Test
  fun `creating a problem does not require Kotlin to be enabled`() {
    val (_, session) = login(UserRole.ADMIN)
    val python = createLanguage("python-${UUID.randomUUID()}")
    val kotlin = entClient.languages.query {
      where(Language.key eq "kotlin")
    }.firstOrNull(fixtureContext).getOrThrow()!!
    entClient.languages.update(kotlin.id) { enabled = false }.save(fixtureContext).getOrThrow()
    try {
      val input = input() + ("languageConfigurations" to listOf(configuration(python.key, "pass", "solution.py")))
      val result = mutate(input, session)
      assertFalse(result.has("errors"), result.toString())
      assertEquals(python.key, result["data"]["createProblem"]["languageConfigurations"][0]["language"]["key"].asString())
    } finally {
      entClient.languages.update(kotlin.id) { enabled = true }.save(fixtureContext).getOrThrow()
    }
  }

  @Test
  fun `invalid unavailable and duplicate language configurations do not create partial problems`() {
    val (_, session) = login(UserRole.ADMIN)
    val disabled = createLanguage("disabled-${UUID.randomUUID()}", enabled = false)
    val valid = configuration()
    val invalidConfigurations = listOf(
      emptyList(),
      listOf(valid, valid),
      listOf(valid, configuration("missing-${UUID.randomUUID()}")),
      listOf(valid, configuration(disabled.key)),
      listOf(valid + ("starterCode" to " ")),
      listOf(valid + ("solutionFilename" to "")),
      listOf(valid + ("solutionFilename" to "../Solution.kt")),
    )
    for (configurations in invalidConfigurations) {
      val input = input() + ("languageConfigurations" to configurations)
      val result = mutate(input, session)
      assertTrue(result.has("errors"), result.toString())
      assertNull(findProblem(input["slug"] as String))
    }
  }

  @Test
  fun `invalid content and duplicate slugs are reported without partial problems`() {
    val (_, session) = login(UserRole.ADMIN)
    val invalidInputs = listOf(
      input() + ("title" to " "),
      input() + ("slug" to "create"),
      input() + ("slug" to "Invalid Slug"),
      input() + ("statementMarkdown" to " "),
      input() + ("examples" to emptyList<Any>()),
      input() + ("examples" to listOf(mapOf("inputJson" to "{broken", "expectedOutputJson" to "null"))),
      input() + ("examples" to listOf(mapOf("inputJson" to "null", "expectedOutputJson" to "{broken"))),
      input() + ("examples" to listOf(mapOf(
        "inputJson" to "null",
        "expectedOutputJson" to "null",
        "explanationMarkdown" to "x".repeat(10_001),
      ))),
    )
    for (input in invalidInputs) {
      val result = mutate(input, session)
      assertTrue(result.has("errors"), result.toString())
      assertEquals("BAD_REQUEST", result["errors"][0]["extensions"]["errorType"].asString())
      assertNull(findProblem(input["slug"] as String))
    }

    val input = input()
    assertFalse(mutate(input, session).has("errors"))
    val duplicate = mutate(input + ("title" to "Replacement title"), session)
    assertTrue(duplicate["errors"][0]["message"].asString().contains("slug already exists"), duplicate.toString())
    assertEquals("Create a pair", findProblem(input["slug"] as String)!!.title)
  }

  @Test
  fun `admin edits existing content while preserving identity and hidden cases`() {
    val (_, session) = login(UserRole.ADMIN)
    val created = mutate(input(), session)["data"]["createProblem"]
    val slug = created["slug"].asString()
    val before = findProblem(slug)!!
    val hidden = entClient.testCases.create {
      problemId = before.id
      position = 8
      visibility = TestCaseVisibility.HIDDEN
      inputJson = JsonPrimitive("private-input")
      expectedOutputJson = JsonNull
    }.saveAndLoad(fixtureContext).getOrThrow()

    val edit = editInput(created) + mapOf(
      "title" to "  Revised pair  ",
      "statementMarkdown" to "Updated statement.",
      "difficulty" to "HARD",
      "languageConfigurations" to listOf(mapOf(
        "id" to created["languageConfigurations"][0]["id"].asString(),
        "starterCode" to "class Solution { fun solve() = 42 }",
        "solutionFilename" to "Pair.kt",
      )),
      "examples" to created["examples"].toList().reversed().map { example ->
        mapOf(
          "id" to example["id"].asString(),
          "inputJson" to "[1,2]",
          "expectedOutputJson" to "null",
          "explanationMarkdown" to "Updated example.",
        )
      },
    )

    val result = update(slug, edit, session)
    assertFalse(result.has("errors"), result.toString())
    assertEquals("UpdateProblemSuccess", result["data"]["updateProblem"]["__typename"].asString())
    val updated = result["data"]["updateProblem"]["problem"]
    assertEquals(created["id"], updated["id"])
    assertEquals(slug, updated["slug"].asString())
    assertEquals("Revised pair", updated["title"].asString())
    assertEquals("Updated statement.", updated["statementMarkdown"].asString())
    assertEquals("HARD", updated["difficulty"].asString())
    assertEquals(created["languageConfigurations"][0]["id"], updated["languageConfigurations"][0]["id"])
    assertEquals("Pair.kt", updated["languageConfigurations"][0]["solutionFilename"].asString())
    assertEquals("class Solution { fun solve() = 42 }", updated["languageConfigurations"][0]["starterCode"].asString())
    assertEquals(created["examples"].toList().map { it["id"] }, updated["examples"].toList().map { it["id"] })
    assertEquals(created["examples"].toList().map { it["position"] }, updated["examples"].toList().map { it["position"] })
    assertTrue(updated["examples"].toList().all { it["expectedOutputJson"].asString() == "null" })
    assertFalse(updated.toString().contains("private-input"))

    val after = findProblem(slug)!!
    assertEquals(before.createdByUserId, after.createdByUserId)
    assertEquals(before.createdAt, after.createdAt)
    assertEquals(before.publishedAt, after.publishedAt)
    assertEquals(hidden.inputJson, entClient.testCases.findById(fixtureContext, hidden.id).getOrThrow()!!.inputJson)
  }

  @Test
  fun `edits require current admin permissions through GraphQL and Ent`() {
    val (adminId, admin) = login(UserRole.ADMIN)
    val (_, ordinary) = login(UserRole.USER)
    val created = mutate(input(), admin)["data"]["createProblem"]
    val slug = created["slug"].asString()
    val edit = editInput(created) + ("title" to "Unauthorized edit")

    for (session in listOf(null, ordinary)) {
      assertUpdateFailure(update(slug, edit, session), "UpdateProblemForbidden")
    }

    val problem = findProblem(slug)!!
    SecurityContextHolder.setContext(sessionContext(admin))
    try {
      assertThrows(EntMutationPrivacyDeniedException::class.java) {
        entClient.problems.update(problem.id) { title = "Spoofed viewer" }
          .save(ViewerContext(Viewer.User(adminId + 1))).getOrThrow()
      }
    } finally {
      SecurityContextHolder.clearContext()
    }

    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtureContext).getOrThrow()
    assertUpdateFailure(update(slug, edit, admin), "UpdateProblemForbidden")
    assertEquals(created["title"].asString(), findProblem(slug)!!.title)
  }

  @Test
  fun `invalid edits and foreign child ids leave the entire problem unchanged`() {
    val (_, session) = login(UserRole.ADMIN)
    val created = mutate(input(), session)["data"]["createProblem"]
    val other = mutate(input(), session)["data"]["createProblem"]
    val slug = created["slug"].asString()
    val original = editInput(created)
    val edited = original + ("title" to "Should roll back")
    val configuration = mapOf(
      "id" to created["languageConfigurations"][0]["id"].asString(),
      "starterCode" to "Changed stencil",
      "solutionFilename" to "Solution.kt",
    )
    val examples = created["examples"].toList().map { example ->
      mapOf(
        "id" to example["id"].asString(),
        "inputJson" to example["inputJson"].asString(),
        "expectedOutputJson" to example["expectedOutputJson"].asString(),
      )
    }
    val invalidEdits = listOf(
      "UpdateProblemValidationFailure" to (edited + ("title" to " ")),
      "UpdateProblemContentChanged" to (edited + ("languageConfigurations" to emptyList<Any>())),
      "UpdateProblemContentChanged" to (edited + ("examples" to listOf(examples[0], examples[0]))),
      "UpdateProblemValidationFailure" to (edited +
        ("languageConfigurations" to listOf(configuration + ("starterCode" to " ")))),
      "UpdateProblemValidationFailure" to (edited +
        ("languageConfigurations" to listOf(configuration + ("id" to "not-an-id")))),
      "UpdateProblemValidationFailure" to (edited +
        ("languageConfigurations" to listOf(configuration + ("id" to created["id"].asString())))),
      "UpdateProblemContentChanged" to (edited + ("languageConfigurations" to listOf(configuration +
        ("id" to other["languageConfigurations"][0]["id"].asString())))),
      "UpdateProblemContentChanged" to (edited +
        ("examples" to listOf(examples[0], examples[1] + ("id" to other["examples"][0]["id"].asString())))),
      "UpdateProblemValidationFailure" to (edited + mapOf(
        "languageConfigurations" to listOf(configuration),
        "examples" to listOf(examples[0], examples[1] + ("expectedOutputJson" to "{broken")),
      )),
      "UpdateProblemValidationFailure" to (edited + mapOf(
        "languageConfigurations" to listOf(configuration),
        "examples" to listOf(
          examples[0] + ("inputJson" to "[42]"),
          examples[1] + ("explanationMarkdown" to "x".repeat(10_001)),
        ),
      )),
    )

    for ((failureType, input) in invalidEdits) {
      val result = update(slug, input, session)
      assertUpdateFailure(result, failureType)
      assertEquals(created, fetchProblem(slug))
    }

    val missing = update("missing-${UUID.randomUUID()}", original, session)
    assertUpdateFailure(missing, "UpdateProblemNotFound")
  }

  private fun assertUpdateFailure(response: JsonNode, type: String) {
    assertFalse(response.has("errors"), response.toString())
    val result = response["data"]["updateProblem"]
    assertEquals(type, result["__typename"].asString(), response.toString())
    assertTrue(result["message"].asString().isNotBlank(), response.toString())
  }

  private fun assertRouteAccess(session: MockHttpSession?, location: String?) {
    val routes = mapOf(
      "CreateProblem" to "/problem/create",
      "EditProblem" to "/problem/two-sum/edit",
    )

    for ((routeId, path) in routes) {
      val request = get(path)
      session?.let { request.session(it) }
      val response = mvc.perform(request).andReturn().response
      assertEquals(if (location == null) 200 else 302, response.status)
      assertEquals(location, response.redirectedUrl)

      val decisionRequest = get("/__spa/route-decision")
        .param("applicationId", "app")
        .param("routeId", routeId)
      if (routeId == "EditProblem") decisionRequest.param("parameters.slug", "two-sum")
      session?.let { decisionRequest.session(it) }
      val decision = objectMapper.readTree(mvc.perform(decisionRequest).andReturn().response.contentAsString)
      assertEquals(if (location == null) 200 else 302, decision["statusCode"].asInt())
      if (location != null) assertEquals(location, decision["location"].asString())
    }
  }

  private fun sessionContext(session: MockHttpSession): SecurityContext =
    session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext

  private fun createLanguage(key: String, enabled: Boolean = true): Language = entClient.languages.create {
    this.key = key
    displayName = key
    this.enabled = enabled
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

  private fun input(): Map<String, Any> = mapOf(
    "slug" to "test-${UUID.randomUUID()}",
    "title" to "Create a pair",
    "statementMarkdown" to "Find a pair.\n\nReturn its indices.",
    "difficulty" to "EASY",
    "languageConfigurations" to listOf(configuration()),
    "examples" to listOf(
      mapOf("inputJson" to "{\"nums\":[2,7],\"target\":9}", "expectedOutputJson" to "[0,1]"),
      mapOf("inputJson" to "[]", "expectedOutputJson" to "null", "explanationMarkdown" to "No pair."),
    ),
  )

  private fun configuration(
    languageKey: String = "kotlin",
    starterCode: String = "class Solution { fun solve(): IntArray = TODO() }",
    solutionFilename: String = "Solution.kt",
  ): Map<String, String> = mapOf(
    "languageKey" to languageKey,
    "starterCode" to starterCode,
    "solutionFilename" to solutionFilename,
  )

  private fun mutate(input: Map<String, Any>, session: MockHttpSession?): JsonNode = graphql(
    """
      mutation Create(${'$'}input: CreateProblemInput!) {
        createProblem(input: ${'$'}input) {
          $PROBLEM_FIELDS
        }
      }
    """.trimIndent(),
    session,
    mapOf("input" to input),
  )

  private fun editInput(problem: JsonNode): Map<String, Any> = mapOf(
    "title" to problem["title"].asString(),
    "statementMarkdown" to problem["statementMarkdown"].asString(),
    "difficulty" to problem["difficulty"].asString(),
    "languageConfigurations" to problem["languageConfigurations"].toList().map { configuration ->
      mapOf(
        "id" to configuration["id"].asString(),
        "starterCode" to configuration["starterCode"].asString(),
        "solutionFilename" to configuration["solutionFilename"].asString(),
      )
    },
    "examples" to problem["examples"].toList().map { example ->
      mapOf(
        "id" to example["id"].asString(),
        "inputJson" to example["inputJson"].asString(),
        "expectedOutputJson" to example["expectedOutputJson"].asString(),
        "explanationMarkdown" to example["explanationMarkdown"].takeUnless { it.isNull }?.asString(),
      )
    },
  )

  private fun update(slug: String, input: Map<String, Any>, session: MockHttpSession?): JsonNode = graphql(
    """
      mutation Edit(${'$'}slug: String!, ${'$'}input: UpdateProblemInput!) {
        updateProblem(slug: ${'$'}slug, input: ${'$'}input) {
          __typename
          ... on UpdateProblemSuccess { problem { $PROBLEM_FIELDS } }
          ... on UpdateProblemFailure { message }
        }
      }
    """.trimIndent(),
    session,
    mapOf("slug" to slug, "input" to input),
  )

  private fun fetchProblem(slug: String): JsonNode = graphql(
    "query { problem(slug: \"$slug\") { $PROBLEM_FIELDS } }"
  )["data"]["problem"]

  private fun graphql(query: String, session: MockHttpSession? = null, variables: Map<String, Any> = emptyMap()): JsonNode {
    val request = post("/graphql")
      .contentType("application/json")
      .cookie(Cookie("XSRF-TOKEN", "test-token"))
      .header("X-XSRF-TOKEN", "test-token")
      .content(objectMapper.writeValueAsString(mapOf("query" to query, "variables" to variables)))
    session?.let { request.session(it) }
    var result: MvcResult = mvc.perform(request).andReturn()
    if (result.request.isAsyncStarted) result = mvc.perform(asyncDispatch(result)).andReturn()
    assertEquals(200, result.response.status, result.response.contentAsString)
    return objectMapper.readTree(result.response.contentAsString)
  }

  private fun findProblem(slug: String): Problem? = entClient.problems.query {
    where(Problem.slug eq slug)
  }.firstOrNull(fixtureContext).getOrThrow()

  companion object {
    private const val PROBLEM_FIELDS = """
      id slug title statementMarkdown difficulty
      languageConfigurations { id starterCode solutionFilename language { key } }
      examples { id position inputJson expectedOutputJson explanationMarkdown }
    """

    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

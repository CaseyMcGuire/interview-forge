package com.application.graphql

import com.application.ent.EntClient
import com.application.graphql.types.Tag
import com.application.schema.UserRole
import com.application.security.UserIdPrincipal
import com.netflix.graphql.dgs.DgsQueryExecutor
import entkt.runtime.privacy.ViewerContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import com.application.graphql.types.Problem as GraphqlProblem

@Testcontainers
@SpringBootTest
class TagDataFetcherIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect isolated tag API fixtures",
  )

  @Autowired lateinit var entClient: EntClient
  @Autowired lateinit var queryExecutor: DgsQueryExecutor
  @Autowired lateinit var objectMapper: ObjectMapper
  @Autowired lateinit var globalIdUtil: GlobalIdUtil

  private var adminId = 0L

  @BeforeEach
  fun signInAsAdmin() {
    adminId = createUser(UserRole.ADMIN)
    signIn(adminId)
  }

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `public tag catalog uses display name and slug order with opaque IDs`() {
    val prefix = "catalog-${UUID.randomUUID()}"
    val last = createTag("$prefix-z", "$prefix Trees")
    val second = createTag("$prefix-b", "$prefix Concurrency")
    val first = createTag("$prefix-a", "$prefix Concurrency")
    SecurityContextHolder.clearContext()

    val tags = graphql("{ tags { id slug displayName } }")["tags"]
      .filter { it["slug"].asString().startsWith(prefix) }

    assertEquals(listOf(first, second, last), tags)
    for (tag in tags) {
      val stored = entClient.tags.indexes.slug(tag["slug"].asString()).find(fixtures).getOrThrow()!!
      assertEquals(globalIdUtil.toGlobalId(Tag::class, stored.id), tag["id"].asString())
    }
  }

  @Test
  fun `administrators can rename a tag and deleting it removes problem assignments`() {
    val tag = createTag()
    val id = tag["id"].asString()
    val problem = createProblem(problemInput(listOf(id)))

    val updated = updateTag(id, "Two Pointers")
    assertEquals("UpdateTagSuccess", updated["__typename"].asString())
    assertEquals(id, updated["tag"]["id"].asString())
    assertEquals(tag["slug"], updated["tag"]["slug"])
    assertEquals("Two Pointers", updated["tag"]["displayName"].asString())
    assertEquals(updated["tag"], readProblem(problem["slug"].asString())["tags"][0])

    val deleted = deleteTag(id)
    assertEquals("DeleteTagSuccess", deleted["__typename"].asString())
    assertEquals(id, deleted["deletedTagId"].asString())
    assertTrue(readProblem(problem["slug"].asString())["tags"].isEmpty)
    assertEquals("TagNotFound", deleteTag(id)["__typename"].asString())
  }

  @Test
  fun `duplicate slugs and invalid content return field validation failures`() {
    val tag = createTag()
    val duplicate = mutate(CREATE_TAG, "createTag", mapOf(
      "slug" to tag["slug"].asString(),
      "displayName" to "Different label",
    ))
    assertValidation(duplicate, "TagValidationFailure", "slug")

    for ((field, invalidValue) in listOf("slug" to "Two Pointers", "displayName" to " ")) {
      val input = mapOf("slug" to "tag-${UUID.randomUUID()}", "displayName" to "Tree") +
        (field to invalidValue)
      assertValidation(mutate(CREATE_TAG, "createTag", input), "TagValidationFailure", field)
    }

    assertValidation(updateTag(tag["id"].asString(), " "), "TagValidationFailure", "displayName")
    val stored = entClient.tags.indexes.slug(tag["slug"].asString()).find(fixtures).getOrThrow()!!
    assertEquals(tag["displayName"].asString(), stored.displayName)
  }

  @Test
  fun `tag mutations distinguish invalid IDs from missing tags`() {
    val invalidIds = listOf(
      "not-an-id",
      globalIdUtil.toGlobalId(GraphqlProblem::class, 1),
      globalIdUtil.toGlobalId(Tag::class, 0),
    )
    for (id in invalidIds) {
      assertValidation(updateTag(id, "Tree"), "TagValidationFailure", "id")
      assertValidation(deleteTag(id), "TagValidationFailure", "id")
    }

    val missing = globalIdUtil.toGlobalId(Tag::class, Long.MAX_VALUE)
    assertEquals("TagNotFound", updateTag(missing, "Tree")["__typename"].asString())
    assertEquals("TagNotFound", deleteTag(missing)["__typename"].asString())
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `anonymous and ordinary users cannot change tags or assignments`(authenticated: Boolean) {
    val tag = createTag()
    val problem = createProblem(problemInput(listOf(tag["id"].asString())))
    SecurityContextHolder.clearContext()
    if (authenticated) {
      signIn(createUser(UserRole.USER))
    }

    assertTagMutationsForbidden(tag["id"].asString())
    val update = updateProblem(problem["id"].asString(), mapOf("tagIds" to emptyList<String>()))
    assertEquals("ProblemForbidden", update["__typename"].asString())
    assertEquals(problem["tags"], readProblem(problem["slug"].asString())["tags"])
  }

  @Test
  fun `revoking administrator privileges prevents tag mutations for an existing identity`() {
    val tag = createTag()
    entClient.users.update(adminId) { role = UserRole.USER }.save(fixtures).getOrThrow()

    assertTagMutationsForbidden(tag["id"].asString())
  }

  @Test
  fun `problem creation stores unique ordered tags and exposes them on public queries`() {
    val prefix = UUID.randomUUID().toString()
    val last = createTag(displayName = "$prefix Tree")
    val first = createTag(displayName = "$prefix Concurrency")
    val input = problemInput(listOf(last["id"].asString(), first["id"].asString(), first["id"].asString()))
    val problem = createProblem(input)

    assertEquals(listOf(first, last), problem["tags"].toList())
    SecurityContextHolder.clearContext()
    assertEquals(problem, readProblem(problem["slug"].asString()))

    val listed = graphql(
      """query(${'$'}search: String!) {
        problems(filters: {search: ${'$'}search}) { edges { node { id tags { id slug displayName } } } }
      }""".trimIndent(),
      mapOf("search" to input.getValue("title")),
    )["problems"]["edges"].single()["node"]
    assertEquals(problem["id"], listed["id"])
    assertEquals(problem["tags"], listed["tags"])
  }

  @Test
  fun `omitted create tags default to an empty list`() {
    val problem = createProblem(problemInput().minus("tagIds"))
    assertTrue(problem["tags"].isEmpty)
  }

  @Test
  fun `problem updates preserve replace and clear assignments`() {
    val first = createTag()
    val second = createTag()
    val problem = createProblem(problemInput(listOf(first["id"].asString())))
    val id = problem["id"].asString()

    val omitted = updateProblem(id, mapOf("title" to "Renamed problem"))
    assertEquals("UpdateProblemSuccess", omitted["__typename"].asString())
    assertEquals(problem["tags"], omitted["problem"]["tags"])

    val unchanged = updateProblem(id, mapOf("tagIds" to null))
    assertEquals(problem["tags"], unchanged["problem"]["tags"])

    val replaced = updateProblem(id, mapOf("tagIds" to listOf(second["id"].asString(), second["id"].asString())))
    assertEquals(listOf(second), replaced["problem"]["tags"].toList())
    assertEquals("Renamed problem", replaced["problem"]["title"].asString())

    val cleared = updateProblem(id, mapOf("tagIds" to emptyList<String>()))
    assertTrue(cleared["problem"]["tags"].isEmpty)
    assertTrue(readProblem(problem["slug"].asString())["tags"].isEmpty)
  }

  @Test
  fun `invalid assignments preserve problem fields and existing relationships`() {
    val originalTag = createTag()
    val newTag = createTag()
    val problem = createProblem(problemInput(listOf(originalTag["id"].asString())))
    val id = problem["id"].asString()
    val missing = globalIdUtil.toGlobalId(Tag::class, Long.MAX_VALUE)

    val result = updateProblem(id, mapOf(
      "title" to "Must roll back",
      "tagIds" to listOf(newTag["id"].asString(), missing),
    ))

    assertValidation(result, "ProblemValidationFailure", "tagIds")
    assertEquals(problem, readProblem(problem["slug"].asString()))
  }

  @Test
  fun `problem creation rolls back tags and content when a tag or example is invalid`() {
    val tag = createTag()
    val missing = globalIdUtil.toGlobalId(Tag::class, Long.MAX_VALUE)
    val tagId = globalIdUtil.fromGlobalIdOrNull(tag["id"].asString(), Tag::class)!!
    val invalidInputs = listOf(
      problemInput(listOf(tag["id"].asString(), missing)),
      problemInput(listOf(tag["id"].asString())) +
        ("examples" to listOf(mapOf("inputJson" to "{", "expectedOutputJson" to "0"))),
    )

    for (input in invalidInputs) {
      val result = queryExecutor.execute(CREATE_PROBLEM, mapOf("input" to input))
      assertFalse(result.errors.isEmpty(), "Expected problem creation to fail")
      assertEquals("BAD_REQUEST", result.errors.first().extensions["errorType"].toString())
      assertNull(entClient.problems.indexes.slug(input.getValue("slug") as String).find(fixtures).getOrThrow())
      assertTrue(entClient.problemTags.indexes.byTag(tagId).query().all(fixtures).getOrThrow().isEmpty())
    }
  }

  @Test
  fun `malformed and wrong-type assignment IDs are field errors`() {
    val problem = createProblem(problemInput())
    val invalidIds = listOf("invalid", problem["id"].asString())

    for (id in invalidIds) {
      val result = updateProblem(problem["id"].asString(), mapOf("tagIds" to listOf(id)))
      assertValidation(result, "ProblemValidationFailure", "tagIds[0]")

      val input = problemInput(listOf(id))
      val creation = queryExecutor.execute(CREATE_PROBLEM, mapOf("input" to input))
      assertFalse(creation.errors.isEmpty())
      assertNull(entClient.problems.indexes.slug(input.getValue("slug") as String).find(fixtures).getOrThrow())
    }
  }

  @Test
  fun `tags do not make an archived problem readable or editable`() {
    val tag = createTag()
    val problem = createProblem(problemInput(listOf(tag["id"].asString())))
    val databaseId = globalIdUtil.fromGlobalIdOrNull(problem["id"].asString(), GraphqlProblem::class)!!
    entClient.problems.update(databaseId) { archivedAt = Instant.now() }.save(fixtures).getOrThrow()

    val result = updateProblem(problem["id"].asString(), mapOf("tagIds" to emptyList<String>()))
    assertEquals("ProblemNotFound", result["__typename"].asString())

    SecurityContextHolder.clearContext()
    assertTrue(readProblem(problem["slug"].asString()).isNull)
  }

  private fun assertTagMutationsForbidden(id: String) {
    val creation = mutate(CREATE_TAG, "createTag", mapOf(
      "slug" to "forbidden-${UUID.randomUUID()}",
      "displayName" to "Forbidden",
    ))
    assertEquals("TagForbidden", creation["__typename"].asString())
    assertEquals("TagForbidden", updateTag(id, "Forbidden")["__typename"].asString())
    assertEquals("TagForbidden", deleteTag(id)["__typename"].asString())
  }

  private fun assertValidation(result: JsonNode, type: String, field: String) {
    assertEquals(type, result["__typename"].asString(), result.toString())
    assertTrue(result["fieldErrors"].any { it["field"].asString() == field }, result.toString())
  }

  private fun createTag(
    slug: String = "tag-${UUID.randomUUID()}",
    displayName: String = "Tree",
  ): JsonNode {
    val result = mutate(CREATE_TAG, "createTag", mapOf("slug" to slug, "displayName" to displayName))
    assertEquals("CreateTagSuccess", result["__typename"].asString(), result.toString())
    return result["tag"]
  }

  private fun updateTag(id: String, displayName: String): JsonNode = mutate(
    UPDATE_TAG, "updateTag", mapOf("id" to id, "displayName" to displayName),
  )

  private fun deleteTag(id: String): JsonNode = mutate(DELETE_TAG, "deleteTag", mapOf("id" to id))

  private fun createProblem(input: Map<String, Any>): JsonNode =
    graphql(CREATE_PROBLEM, mapOf("input" to input))["createProblem"]

  private fun updateProblem(id: String, fields: Map<String, Any?>): JsonNode =
    mutate(UPDATE_PROBLEM, "updateProblem", mapOf("id" to id) + fields)

  private fun readProblem(slug: String): JsonNode = graphql(
    """query(${'$'}slug: String!) { problem(slug: ${'$'}slug) { id slug title tags { id slug displayName } } }""",
    mapOf("slug" to slug),
  )["problem"]

  private fun problemInput(tagIds: List<String> = emptyList()): Map<String, Any> = mapOf(
    "slug" to "tagged-${UUID.randomUUID()}",
    "title" to "Tagged problem ${UUID.randomUUID()}",
    "statementMarkdown" to "Return the input.",
    "difficulty" to "EASY",
    "tagIds" to tagIds,
    "languageConfigurations" to listOf(mapOf("languageKey" to "kotlin", "starterCode" to "class Solution")),
    "examples" to listOf(mapOf("inputJson" to "0", "expectedOutputJson" to "0")),
  )

  private fun mutate(query: String, field: String, input: Map<String, Any?>): JsonNode =
    graphql(query, mapOf("input" to input))[field]

  private fun graphql(query: String, variables: Map<String, Any?> = emptyMap()): JsonNode {
    val result = queryExecutor.execute(query, variables)
    assertTrue(result.errors.isEmpty(), result.errors.toString())
    return objectMapper.valueToTree(result.getData<Any>())
  }

  private fun createUser(role: UserRole): Long = entClient.users.create {
    email = "tag-api-${UUID.randomUUID()}@example.com"
    hashedPassword = "unused"
    this.role = role
  }.saveAndLoad(fixtures).getOrThrow().id

  private fun signIn(userId: Long) {
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
      UserIdPrincipal(userId), null, emptyList(),
    )
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))

    private val CREATE_TAG = """
      mutation(${'$'}input: CreateTagInput!) {
        createTag(input: ${'$'}input) {
          __typename
          ... on CreateTagSuccess { tag { id slug displayName } }
          ... on TagValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent()

    private val UPDATE_TAG = """
      mutation(${'$'}input: UpdateTagInput!) {
        updateTag(input: ${'$'}input) {
          __typename
          ... on UpdateTagSuccess { tag { id slug displayName } }
          ... on TagValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent()

    private val DELETE_TAG = """
      mutation(${'$'}input: DeleteTagInput!) {
        deleteTag(input: ${'$'}input) {
          __typename
          ... on DeleteTagSuccess { deletedTagId }
          ... on TagValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent()

    private val CREATE_PROBLEM = """
      mutation(${'$'}input: CreateProblemInput!) {
        createProblem(input: ${'$'}input) { id slug title tags { id slug displayName } }
      }
    """.trimIndent()

    private val UPDATE_PROBLEM = """
      mutation(${'$'}input: UpdateProblemInput!) {
        updateProblem(input: ${'$'}input) {
          __typename
          ... on UpdateProblemSuccess { problem { id slug title tags { id slug displayName } } }
          ... on ProblemValidationFailure { fieldErrors { field message } }
        }
      }
    """.trimIndent()
  }
}

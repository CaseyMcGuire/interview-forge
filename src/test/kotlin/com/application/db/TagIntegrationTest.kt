package com.application.db

import com.application.ent.EntClient
import com.application.ent.Problem
import com.application.ent.Tag
import com.application.schema.ProblemDifficulty
import com.application.schema.UserRole
import com.application.security.UserIdPrincipal
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntConstraintViolationException
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntValidationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.time.Instant
import java.util.UUID

@Testcontainers
@SpringBootTest
class TagIntegrationTest {
  private val fixtures = ViewerContext.privacyBypass_DANGEROUS(
    "Seed and inspect isolated problem-tag fixtures",
  )
  private val anonymous = ViewerContext(Viewer.Anonymous)

  @Autowired
  lateinit var entClient: EntClient

  private var adminId = 0L
  private lateinit var admin: ViewerContext

  @BeforeEach
  fun signInAsAdmin() {
    adminId = createUser(UserRole.ADMIN)
    admin = signIn(adminId)
  }

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `tags are publicly readable and administrators can rename them`() {
    val tag = createTag()
    val renamed = "Two Pointers"

    entClient.tags.update(tag.id) {
      displayName = renamed
    }.save(admin).getOrThrow()

    SecurityContextHolder.clearContext()
    val stored = entClient.tags.findById(anonymous, tag.id).getOrThrow()!!

    assertEquals(tag.id, stored.id)
    assertEquals(tag.slug, stored.slug)
    assertEquals(renamed, stored.displayName)
    assertEquals(tag.createdAt, stored.createdAt)
    assertNotNull(stored.updatedAt)
    assertEquals(stored.id, entClient.tags.indexes.slug(stored.slug).find(anonymous).getOrThrow()!!.id)
  }

  @Test
  fun `tag slugs are unique and display names can be shared`() {
    val tag = createTag()
    val other = createTag()

    assertThrows(EntConstraintViolationException::class.java) {
      entClient.tags.create {
        slug = tag.slug
        displayName = "Another label"
      }.save(admin).getOrThrow()
    }

    entClient.tags.update(other.id) { displayName = tag.displayName }.save(admin).getOrThrow()

    val stored = entClient.tags.findById(anonymous, other.id).getOrThrow()!!
    assertEquals(tag.displayName, stored.displayName)
    assertEquals(other.slug, stored.slug)
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "Tree", "two pointer", "-tree", "tree-", "two--pointer", "tree!"])
  fun `invalid slugs are rejected`(slug: String) {
    assertInvalidSlug(slug)
  }

  @ParameterizedTest
  @ValueSource(strings = ["", " ", " Tree", "Tree "])
  fun `invalid display names are rejected on creation and update`(displayName: String) {
    assertInvalidDisplayName(displayName)
  }

  @Test
  fun `tag slugs and display names cannot exceed 100 characters`() {
    assertInvalidSlug("a".repeat(101))
    assertInvalidDisplayName("a".repeat(101))
  }

  @Test
  fun `problems share tags and relationship helpers add remove and replace links`() {
    val first = createProblem()
    val second = createProblem()
    val tree = createTag()
    val concurrency = createTag()

    entClient.withTransaction { tx ->
      tx.problems.update(first.id) { tags.add(tree.id) }.save(admin).getOrThrow()
      tx.problems.update(second.id) { tags.add(tree.id) }.save(admin).getOrThrow()
    }.getOrThrow()

    assertEquals(setOf(first.id, second.id), problemIds(tree))
    assertEquals(setOf(tree.id), tagIds(first))

    entClient.withTransaction { tx ->
      tx.problems.update(first.id) {
        tags.set(listOf(tree.id, concurrency.id))
      }.save(admin).getOrThrow()
      tx.tags.update(tree.id) { problems.remove(second.id) }.save(admin).getOrThrow()
      tx.tags.update(concurrency.id) { problems.add(second.id) }.save(admin).getOrThrow()
    }.getOrThrow()

    assertEquals(setOf(tree.id, concurrency.id), tagIds(first))
    assertEquals(setOf(concurrency.id), tagIds(second))
    assertEquals(setOf(first.id), problemIds(tree))

    entClient.withTransaction { tx ->
      tx.problems.update(first.id) { tags.set(emptyList()) }.save(admin).getOrThrow()
    }.getOrThrow()

    assertEquals(emptySet<Long>(), tagIds(first))
    assertEquals(setOf(second.id), problemIds(concurrency))
  }

  @Test
  fun `repeated adds are idempotent and the database rejects duplicate links`() {
    val problem = createProblem()
    val tag = createTag()

    repeat(2) {
      entClient.withTransaction { tx ->
        tx.problems.update(problem.id) { tags.add(tag.id) }.save(admin).getOrThrow()
      }.getOrThrow()
    }

    val links = entClient.problemTags.indexes.byTag(tag.id).query().all(fixtures).getOrThrow()
    assertEquals(listOf(problem.id), links.map { it.problemId })

    assertThrows(EntConstraintViolationException::class.java) {
      entClient.problemTags.create {
        problemId = problem.id
        tagId = tag.id
      }.save(fixtures).getOrThrow()
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `anonymous viewers and ordinary users cannot change tags or links`(authenticated: Boolean) {
    val tag = createTag()
    val problem = createProblem()
    SecurityContextHolder.clearContext()
    val viewer = if (authenticated) signIn(createUser(UserRole.USER)) else anonymous

    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.tags.create {
        slug = "forbidden"
        displayName = "Forbidden"
      }.save(viewer).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.tags.update(tag.id) { displayName = "Forbidden" }.save(viewer).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.tags.deleteById(viewer, tag.id).getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.withTransaction { tx ->
        tx.problems.update(problem.id) { tags.add(tag.id) }.save(viewer).getOrThrow()
      }.getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.withTransaction { tx ->
        tx.tags.update(tag.id) { problems.add(problem.id) }.save(viewer).getOrThrow()
      }.getOrThrow()
    }
    assertThrows(EntMutationPrivacyDeniedException::class.java) {
      entClient.problemTags.create {
        problemId = problem.id
        tagId = tag.id
      }.save(viewer).getOrThrow()
    }

    assertEquals(tag.displayName, entClient.tags.findById(viewer, tag.id).getOrThrow()!!.displayName)
    assertEquals(emptySet<Long>(), tagIds(problem))
  }

  @Test
  fun `tag traversal respects problem visibility`() {
    val published = createProblem()
    val draft = createProblem(publishedAt = null)
    val archived = createProblem(archivedAt = Instant.now())
    val tag = createTag()

    entClient.withTransaction { tx ->
      tx.tags.update(tag.id) {
        problems.set(listOf(published.id, draft.id, archived.id))
      }.save(admin).getOrThrow()
    }.getOrThrow()

    SecurityContextHolder.clearContext()
    assertEquals(setOf(published.id), problemIds(tag))
  }

  @Test
  fun `deleting an endpoint removes only its links`() {
    val first = createProblem()
    val second = createProblem()
    val tag = createTag()

    entClient.withTransaction { tx ->
      tx.tags.update(tag.id) { problems.set(listOf(first.id, second.id)) }.save(admin).getOrThrow()
    }.getOrThrow()

    entClient.problems.deleteById(fixtures, first.id).getOrThrow()
    val remainingLinks = entClient.problemTags.indexes.byTag(tag.id).query().all(fixtures).getOrThrow()

    assertEquals(setOf(second.id), problemIds(tag))
    assertEquals(listOf(second.id), remainingLinks.map { it.problemId })

    entClient.tags.deleteById(admin, tag.id).getOrThrow()

    assertNull(entClient.tags.findById(anonymous, tag.id).getOrThrow())
    assertNotNull(entClient.problems.findById(anonymous, second.id).getOrThrow())
    assertEquals(emptySet<Long>(), tagIds(second))
    assertEquals(0, entClient.problemTags.indexes.byTag(tag.id).query().all(fixtures).getOrThrow().size)
  }

  private fun assertInvalidSlug(slug: String) {
    assertThrows(EntValidationException::class.java) {
      entClient.tags.create {
        this.slug = slug
        displayName = "Valid label"
      }.save(admin).getOrThrow()
    }
  }

  private fun assertInvalidDisplayName(displayName: String) {
    val tag = createTag()

    assertThrows(EntValidationException::class.java) {
      entClient.tags.create {
        slug = "tag-${UUID.randomUUID()}"
        this.displayName = displayName
      }.save(admin).getOrThrow()
    }
    assertThrows(EntValidationException::class.java) {
      entClient.tags.update(tag.id) { this.displayName = displayName }.save(admin).getOrThrow()
    }

    assertEquals(tag.displayName, entClient.tags.findById(anonymous, tag.id).getOrThrow()!!.displayName)
  }

  private fun tagIds(problem: Problem): Set<Long> {
    val loaded = entClient.problems.query {
      where(Problem.id eq problem.id)
      loadTags()
    }.firstOrNull(anonymous).getOrThrow()!!

    return loaded.edges.tags.requireLoaded().map { it.id }.toSet()
  }

  private fun problemIds(tag: Tag): Set<Long> {
    val loaded = entClient.tags.query {
      where(Tag.id eq tag.id)
      loadProblems().filterVisible()
    }.firstOrNull(anonymous).getOrThrow()!!

    return loaded.edges.problems.requireLoaded().map { it.id }.toSet()
  }

  private fun createTag(): Tag = entClient.tags.create {
    slug = "tag-${UUID.randomUUID()}"
    displayName = "Tree"
  }.saveAndLoad(admin).getOrThrow()

  private fun createProblem(
    publishedAt: Instant? = Instant.now(),
    archivedAt: Instant? = null,
  ): Problem = entClient.problems.create {
    slug = "tag-${UUID.randomUUID()}"
    title = "Tagged problem"
    statementMarkdown = "Return the input."
    difficulty = ProblemDifficulty.EASY
    createdByUserId = adminId
    this.publishedAt = publishedAt
    this.archivedAt = archivedAt
  }.saveAndLoad(fixtures).getOrThrow()

  private fun createUser(role: UserRole): Long = entClient.users.create {
    email = "tags-${UUID.randomUUID()}@example.com"
    hashedPassword = "unused"
    this.role = role
  }.saveAndLoad(fixtures).getOrThrow().id

  private fun signIn(userId: Long): ViewerContext {
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
      UserIdPrincipal(userId), null, emptyList(),
    )
    return ViewerContext(Viewer.User(userId))
  }

  companion object {
    @Container
    @ServiceConnection
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}

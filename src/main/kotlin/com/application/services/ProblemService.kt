package com.application.services

import com.application.ent.EntClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.ProblemQuery
import com.application.ent.ProblemQueryScope
import com.application.ent.TestCase
import com.application.schema.TestCaseVisibility
import com.application.schema.ProblemDifficulty
import entkt.query.isNull
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ProblemService(private val entClient: EntClient) {
  // This catalog view uses the same public visibility for signed-in and anonymous visitors.
  private val publicContext = ViewerContext(Viewer.Anonymous)

  fun findPublicProblems(
    first: Int,
    after: ProblemCursor? = null,
    search: String? = null,
    difficulty: ProblemDifficulty? = null,
  ): ProblemPage {
    require(first in 0..100) { "first must be between 0 and 100" }
    var query = publicProblemsQuery(search, difficulty)
    if (after != null) {
      query = query.where(
        (Problem.title gt after.title) or
          ((Problem.title eq after.title) and (Problem.slug gt after.slug))
      )
    }
    val selected = query
      .orderBy(Problem.title.asc())
      .orderBy(Problem.slug.asc())
      .limit(first + 1)
      .all(publicContext)
      .getOrThrow()
    val hasPreviousPage = after != null && publicProblemsQuery(search, difficulty)
      .where(
        (Problem.title lt after.title) or
          ((Problem.title eq after.title) and (Problem.slug lte after.slug))
      )
      .firstOrNull(publicContext)
      .getOrThrow() != null

    return ProblemPage(
      problems = selected.take(first),
      hasNextPage = selected.size > first,
      hasPreviousPage = hasPreviousPage,
    )
  }

  private fun publicProblemsQuery(search: String?, difficulty: ProblemDifficulty?): ProblemQuery =
    entClient.problems.query {
      where(Problem.publishedAt lte Instant.now())
      where(Problem.archivedAt.isNull())
      search?.trim()?.takeIf { it.isNotEmpty() }?.let { where(Problem.title contains it) }
      difficulty?.let { where(Problem.difficulty eq it) }
      loadPublicContent()
    }

  fun findPublicProblemBySlug(slug: String): Problem? =
    entClient.problems.query {
      where(Problem.slug eq slug)
      loadPublicContent()
    }.firstOrNull(publicContext).visibleOrNull().getOrThrow()

  private fun ProblemQueryScope.loadPublicContent() {
    loadLanguageConfigurations {
      where(ProblemLanguage.language.has { where(Language.enabled eq true) })
      loadLanguage()
    }
    loadTestCases {
      where(TestCase.visibility eq TestCaseVisibility.EXAMPLE)
      orderBy(TestCase.position.asc())
    }
  }
}

data class ProblemPage(
  val problems: List<Problem>,
  val hasNextPage: Boolean,
  val hasPreviousPage: Boolean,
)

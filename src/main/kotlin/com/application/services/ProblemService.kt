package com.application.services

import com.application.ent.EntClient
import com.application.ent.Language
import com.application.ent.Problem
import com.application.ent.ProblemLanguage
import com.application.ent.TestCase
import com.application.schema.TestCaseVisibility
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import org.springframework.stereotype.Service

@Service
class ProblemService(private val entClient: EntClient) {
  // This catalog view uses the same public visibility for signed-in and anonymous visitors.
  private val publicContext = ViewerContext(Viewer.Anonymous)

  fun findPublicProblemBySlug(slug: String): Problem? =
    entClient.problems.query {
      where(Problem.slug eq slug)
      loadLanguageConfigurations {
        where(ProblemLanguage.language.has { where(Language.enabled eq true) })
        loadLanguage().filterVisible()
      }.filterVisible()
      loadTestCases {
        where(TestCase.visibility eq TestCaseVisibility.EXAMPLE)
        orderBy(TestCase.position.asc())
      }.filterVisible()
    }.firstOrNull(publicContext).visibleOrNull().getOrThrow()
}

package com.application.config

import com.application.spa.AppSpaApplication
import com.application.generated.spa.routes.app.CreateProblem
import com.application.generated.spa.routes.app.CreateProblemTestCase
import com.application.generated.spa.routes.app.EditProblem
import com.application.generated.spa.routes.app.EditProblemTestCase
import com.application.generated.spa.routes.app.ProblemTestCases
import com.application.security.RequireAdminRouteRule
import com.sparouting.contract.SpaRouteKey
import io.github.caseymcguire.sparouting.spring.config.SinglePageApplicationConfig
import io.github.caseymcguire.sparouting.spring.rules.SpaRouteRule
import io.github.caseymcguire.sparouting.spring.rules.builtin.AllowAll
import org.springframework.stereotype.Component

/**
 * Registers the main `app` SPA with the spa-routing starter, which turns each route defined
 * in [AppSpaApplication] into a GET mapping rendered by [AppSpaHtmlRenderer].
 *
 * Public routes opt in through [AllowAll]; route-specific rules additionally protect
 * authoring on direct loads and `/__spa/route-decision` checks during SPA navigation.
 */
@Component
class AppSpaConfig(private val requireAdminRouteRule: RequireAdminRouteRule) : SinglePageApplicationConfig {
  override val application = AppSpaApplication

  override val rules: List<SpaRouteRule> = listOf(AllowAll())

  override val routeRules: Map<SpaRouteKey, List<SpaRouteRule>> =
    mapOf(
      CreateProblem to listOf(requireAdminRouteRule),
      CreateProblemTestCase to listOf(requireAdminRouteRule),
      ProblemTestCases to listOf(requireAdminRouteRule),
      EditProblemTestCase to listOf(requireAdminRouteRule),
      EditProblem to listOf(requireAdminRouteRule),
    )
}

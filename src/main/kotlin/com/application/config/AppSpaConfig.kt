package com.application.config

import com.application.generated.spa.routes.AppApplicationAccessHandler
import com.application.generated.spa.routes.AppApplicationConfig
import com.application.generated.spa.routes.AppRouteAccessHandlers
import com.application.generated.spa.routes.AppRoutes
import com.application.generated.spa.routes.app.CreateProblemAccessHandler
import com.application.generated.spa.routes.app.CreateProblemTestCaseAccessHandler
import com.application.generated.spa.routes.app.EditProblemAccessHandler
import com.application.generated.spa.routes.app.EditProblemTestCaseAccessHandler
import com.application.generated.spa.routes.app.ProblemTestCasesAccessHandler
import com.application.security.RequireAdminRouteAccess
import com.sparouting.contract.AccessDecision
import com.sparouting.contract.RouteRequest
import com.sparouting.runtime.evaluation.DefaultRouteFailureHandler
import com.sparouting.runtime.evaluation.RouteFailureHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Public application access followed by typed checks on the authoring routes. */
@Configuration(proxyBeanMethods = false)
class AppSpaConfig(
  private val requireAdminRouteAccess: RequireAdminRouteAccess,
  private val htmlRenderer: AppSpaHtmlRenderer,
) {
  @Bean
  fun appApplicationConfig(): AppApplicationConfig = AppApplicationConfig(
    applicationAccessHandler = object : AppApplicationAccessHandler() {
      override fun evaluate(request: RouteRequest): AccessDecision = AccessDecision.Allowed
    },
    routeAccessHandlers = AppRouteAccessHandlers(
      createProblem = CreateProblemAccessHandler { requireAdminRouteAccess.evaluate() },
      editProblem = EditProblemAccessHandler { requireAdminRouteAccess.evaluate() },
      createProblemTestCase = CreateProblemTestCaseAccessHandler { requireAdminRouteAccess.evaluate() },
      problemTestCases = ProblemTestCasesAccessHandler { requireAdminRouteAccess.evaluate() },
      editProblemTestCase = EditProblemTestCaseAccessHandler { requireAdminRouteAccess.evaluate() },
    ),
    htmlRenderer = htmlRenderer,
  )

  @Bean
  fun routeFailureHandler(): RouteFailureHandler = DefaultRouteFailureHandler(
    unknownRouteDestination = AppRoutes.Home(),
    invalidRequestDestination = AppRoutes.Home(),
  )
}

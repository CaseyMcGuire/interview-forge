package com.application.config

import com.application.generated.spa.routes.AppApplicationAccessHandler
import com.application.generated.spa.routes.AppApplicationConfig
import com.application.generated.spa.routes.AppRouteAccessHandlers
import com.application.generated.spa.routes.AppRoutes
import com.application.generated.spa.routes.app.CreateProblemAccessHandler
import com.application.generated.spa.routes.app.CreateProblemRequest
import com.application.generated.spa.routes.app.CreateProblemTestCaseAccessHandler
import com.application.generated.spa.routes.app.CreateProblemTestCaseRequest
import com.application.generated.spa.routes.app.EditProblemAccessHandler
import com.application.generated.spa.routes.app.EditProblemRequest
import com.application.generated.spa.routes.app.EditProblemTestCaseAccessHandler
import com.application.generated.spa.routes.app.EditProblemTestCaseRequest
import com.application.generated.spa.routes.app.ProblemTestCasesAccessHandler
import com.application.generated.spa.routes.app.ProblemTestCasesRequest
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
      createProblem = object : CreateProblemAccessHandler() {
        override fun evaluate(request: CreateProblemRequest): AccessDecision = requireAdminRouteAccess.evaluate()
      },
      editProblem = object : EditProblemAccessHandler() {
        override fun evaluate(request: EditProblemRequest): AccessDecision = requireAdminRouteAccess.evaluate()
      },
      createProblemTestCase = object : CreateProblemTestCaseAccessHandler() {
        override fun evaluate(request: CreateProblemTestCaseRequest): AccessDecision = requireAdminRouteAccess.evaluate()
      },
      problemTestCases = object : ProblemTestCasesAccessHandler() {
        override fun evaluate(request: ProblemTestCasesRequest): AccessDecision = requireAdminRouteAccess.evaluate()
      },
      editProblemTestCase = object : EditProblemTestCaseAccessHandler() {
        override fun evaluate(request: EditProblemTestCaseRequest): AccessDecision = requireAdminRouteAccess.evaluate()
      },
    ),
    htmlRenderer = htmlRenderer,
  )

  @Bean
  fun routeFailureHandler(): RouteFailureHandler = DefaultRouteFailureHandler(
    unknownRouteDestination = AppRoutes.Home(),
    invalidRequestDestination = AppRoutes.Home(),
  )
}

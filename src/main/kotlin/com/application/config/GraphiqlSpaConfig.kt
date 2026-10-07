package com.application.config

import com.application.generated.spa.routes.GraphiqlApplicationAccessHandler
import com.application.generated.spa.routes.GraphiqlApplicationConfig
import com.application.generated.spa.routes.GraphiqlRouteAccessHandlers
import com.application.views.ReactPage
import com.sparouting.contract.AccessDecision
import com.sparouting.contract.HtmlRenderer
import com.sparouting.contract.RouteRequest
import kotlinx.html.link
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Registers the public GraphiQL explorer with its entry-specific stylesheet. */
@Configuration(proxyBeanMethods = false)
class GraphiqlSpaConfig {
  @Bean
  fun graphiqlApplicationConfig(): GraphiqlApplicationConfig = GraphiqlApplicationConfig(
    applicationAccessHandler = object : GraphiqlApplicationAccessHandler() {
      override fun evaluate(request: RouteRequest): AccessDecision = AccessDecision.Allowed
    },
    routeAccessHandlers = GraphiqlRouteAccessHandlers(),
    htmlRenderer = HtmlRenderer { application ->
      ReactPage(bundleName = application.bundleName, pageTitle = "GraphiQL")
        .customHead {
          link(rel = "stylesheet", href = "/bundles/graphiql.css")
        }
        .render()
    },
  )
}

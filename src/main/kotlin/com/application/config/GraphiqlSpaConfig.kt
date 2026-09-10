package com.application.config

import com.application.spa.GraphiqlSpaApplication
import com.application.views.ReactPage
import io.github.caseymcguire.sparouting.spring.config.SinglePageApplicationConfig
import io.github.caseymcguire.sparouting.spring.rules.SpaRouteRule
import io.github.caseymcguire.sparouting.spring.rules.builtin.AllowAll
import kotlinx.html.link
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerResponse

/** Registers the GraphiQL explorer SPA (`/graphiql`) with the spa-routing starter. */
@Component
class GraphiqlSpaConfig : SinglePageApplicationConfig {
  override val application = GraphiqlSpaApplication

  // Application rules are a deny-by-default gate; explicitly serve GraphiQL ungated
  override val rules: List<SpaRouteRule> = listOf(AllowAll())

  // Overrides the shared AppSpaHtmlRenderer because this entry bundles its own CSS
  // (GraphiQL's UI styles), which the page must link explicitly (see AGENTS.md,
  // "How pages get their assets")
  override fun renderHtml(): ServerResponse {
    val html = ReactPage(bundleName = application.bundleName, pageTitle = "GraphiQL")
      .customHead {
        link(rel = "stylesheet", href = "/bundles/graphiql.css")
      }
      .render()
    return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(html)
  }
}

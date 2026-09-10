package com.application.config

import com.application.spa.AppSpaApplication
import io.github.caseymcguire.sparouting.spring.config.SinglePageApplicationConfig
import io.github.caseymcguire.sparouting.spring.rules.SpaRouteRule
import io.github.caseymcguire.sparouting.spring.rules.builtin.AllowAll
import org.springframework.stereotype.Component

/**
 * Registers the main `app` SPA with the spa-routing starter, which turns each route defined
 * in [AppSpaApplication] into a GET mapping rendered by [AppSpaHtmlRenderer].
 *
 * Application rules are a deny-by-default gate (spa-routing 0.2.0), so [AllowAll] is the
 * explicit opt-in serving every route ungated — both direct loads and the
 * `/__spa/route-decision` checks the client makes on in-page navigation. To gate pages
 * (e.g. require login), replace it with your own [SpaRouteRule]s — the client wiring in
 * App.tsx already honors them.
 */
@Component
class AppSpaConfig : SinglePageApplicationConfig {
  override val application = AppSpaApplication

  override val rules: List<SpaRouteRule> = listOf(AllowAll())
}

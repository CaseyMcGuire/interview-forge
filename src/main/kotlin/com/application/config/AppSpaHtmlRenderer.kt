package com.application.config

import com.application.views.ReactPage
import io.github.caseymcguire.sparouting.spring.config.SinglePageApplicationConfig
import io.github.caseymcguire.sparouting.spring.rendering.SpaHtmlRenderer
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerResponse

/**
 * Renders this app's HTML shell for every SPA route served by the spa-routing starter.
 *
 * Overrides the starter's default renderer (via `@ConditionalOnMissingBean`) because this
 * app loads React from an esm.sh import map — the bundles mark react/react-dom as external
 * module imports — and links the StyleX stylesheet. The default renderer emits neither,
 * which would leave the bundles unable to resolve `react`. Delegating to [ReactPage] keeps
 * a single source of truth for the page shell.
 */
@Component
class AppSpaHtmlRenderer : SpaHtmlRenderer {
  override fun render(application: SinglePageApplicationConfig): ServerResponse {
    val title = PAGE_TITLES[application.applicationId] ?: application.name
    val html = ReactPage(bundleName = application.bundleName, pageTitle = title).render()
    return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(html)
  }

  private companion object {
    /** Per-app page titles. The combined `app` bundle serves every client route. */
    val PAGE_TITLES = mapOf(
      "app" to "Home",
    )
  }
}

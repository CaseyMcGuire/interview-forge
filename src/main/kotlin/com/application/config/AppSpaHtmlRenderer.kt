package com.application.config

import com.application.views.ReactPage
import com.sparouting.contract.SinglePageApplicationConfig
import com.sparouting.contract.HtmlRenderer
import org.springframework.stereotype.Component

/**
 * Supplies the main application's HTML shell, including the React import map and
 * StyleX stylesheet. The application config owns this renderer; the starter writes
 * the HTTP response after access is allowed.
 */
@Component
class AppSpaHtmlRenderer : HtmlRenderer {
  override fun render(application: SinglePageApplicationConfig): String {
    val title = PAGE_TITLES[application.id] ?: application.name
    return ReactPage(bundleName = application.bundleName, pageTitle = title).render()
  }

  private companion object {
    /** Per-app page titles. The combined `app` bundle serves every client route. */
    val PAGE_TITLES = mapOf(
      "app" to "Home",
    )
  }
}

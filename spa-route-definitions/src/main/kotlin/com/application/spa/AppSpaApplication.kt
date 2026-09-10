package com.application.spa

import com.sparouting.contract.SpaApplicationDefinition
import com.sparouting.contract.SpaRouteDefinition
import com.sparouting.contract.route

/**
 * The main single-page application. One bundle (`app`) serves the home page and the
 * about/blog/login/register pages; the client routes between them with react-router, so
 * navigation is in-page. The spa-routing starter registers a server GET route for each,
 * all rendering this same bundle, which is what makes deep-linking/reload work.
 */
object AppSpaApplication : SpaApplicationDefinition {
  override val id = "app"
  override val name = "App"
  override val urlPrefix = ""
  override val appRootPath = "./src/main/web-frontend/App"
  override val routes: List<SpaRouteDefinition> = listOf(
    route("", "Home"),
    route("about", "About"),
    route("blog", "Blog"),
    route("login", "Login"),
    route("register", "Register"),
  )
}

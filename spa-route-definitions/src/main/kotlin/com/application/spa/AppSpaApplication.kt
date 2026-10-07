package com.application.spa

import com.sparouting.contract.SinglePageApplicationDefinition
import com.sparouting.contract.RouteDefinition
import com.sparouting.contract.route

/**
 * The main single-page application. One bundle (`app`) serves the home page and the
 * problem/about/blog/login/register pages; the client routes between them with react-router, so
 * navigation is in-page. The spa-routing starter registers a server GET route for each,
 * all rendering this same bundle, which is what makes deep-linking/reload work.
 */
object AppSpaApplication : SinglePageApplicationDefinition {
  override val id = "app"
  override val name = "App"
  override val urlPrefix = ""
  override val appRootPath = "./src/main/web-frontend/App"
  override val routes: List<RouteDefinition> = listOf(
    route("", "Home"),
    route("problems", "Problems"),
    route("problem/create", "CreateProblem", generateAccessHandler = true),
    route("problem/{slug}/edit", "EditProblem", generateAccessHandler = true),
    route("problem/{slug}/test/create", "CreateProblemTestCase", generateAccessHandler = true),
    route("problem/{slug}/tests", "ProblemTestCases", generateAccessHandler = true),
    route("problem/{slug}/test/{id}/edit", "EditProblemTestCase", generateAccessHandler = true),
    route("problem/{slug}", "Problem"),
    route("about", "About"),
    route("blog", "Blog"),
    route("login", "Login"),
    route("register", "Register"),
  )
}

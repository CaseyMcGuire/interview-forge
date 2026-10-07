package com.application.spa

import com.sparouting.contract.SinglePageApplicationDefinition
import com.sparouting.contract.RouteDefinition
import com.sparouting.contract.route

/** The self-hosted GraphiQL explorer, served at `/graphiql`. */
object GraphiqlSpaApplication : SinglePageApplicationDefinition {
  override val id = "graphiql"
  override val name = "Graphiql"
  override val urlPrefix = "graphiql"
  override val appRootPath = "./src/main/web-frontend/pages/GraphiqlPage"
  override val routes: List<RouteDefinition> = listOf(
    route("", "Graphiql"),
  )
}

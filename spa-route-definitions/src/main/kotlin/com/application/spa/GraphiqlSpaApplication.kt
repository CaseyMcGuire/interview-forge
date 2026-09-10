package com.application.spa

import com.sparouting.contract.SpaApplicationDefinition
import com.sparouting.contract.SpaRouteDefinition
import com.sparouting.contract.route

/** The self-hosted GraphiQL explorer, served at `/graphiql`. */
object GraphiqlSpaApplication : SpaApplicationDefinition {
  override val id = "graphiql"
  override val name = "Graphiql"
  override val urlPrefix = "graphiql"
  override val appRootPath = "./src/main/web-frontend/pages/GraphiqlPage"
  override val routes: List<SpaRouteDefinition> = listOf(
    route("", "Graphiql"),
  )
}

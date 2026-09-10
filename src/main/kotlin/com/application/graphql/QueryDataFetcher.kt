package com.application.graphql

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery


@DgsComponent
class QueryDataFetcher {
  @DgsQuery
  fun welcomeMessage() = "Welcome to the application!"
}

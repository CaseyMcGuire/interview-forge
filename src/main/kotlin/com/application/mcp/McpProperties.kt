package com.application.mcp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mcp")
class McpProperties(
  val apiToken: String = "",
  val userId: Long = 0,
  val allowedHosts: List<String> = listOf("localhost:*", "127.0.0.1:*", "[::1]:*"),
  val allowedOrigins: List<String> = listOf("http://localhost:*", "http://127.0.0.1:*", "http://[::1]:*"),
) {
  val enabled: Boolean = apiToken.isNotEmpty() || userId != 0L

  init {
    if (enabled) {
      require(apiToken.length >= 32 && apiToken.none { it.isWhitespace() }) {
        "MCP_API_TOKEN must contain at least 32 characters and no whitespace"
      }
      require(userId > 0) { "MCP_USER_ID must identify an existing administrator" }
      require(allowedHosts.isNotEmpty()) { "MCP allowed hosts must not be empty" }
      require(allowedOrigins.isNotEmpty()) { "MCP allowed origins must not be empty" }
    }
  }
}

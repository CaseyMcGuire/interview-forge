package com.application.mcp

import com.application.security.CurrentUserService
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.SecurityContextHolderFilter
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableConfigurationProperties(McpProperties::class)
class McpConfiguration {
  @Bean
  @Order(1)
  fun mcpSecurityFilterChain(
    http: HttpSecurity,
    properties: McpProperties,
    currentUserService: CurrentUserService,
  ): SecurityFilterChain = http
    .securityMatcher("/mcp", "/mcp/**")
    .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
    .requestCache { it.disable() }
    // This endpoint requires a bearer token; browser endpoints retain their CSRF protection.
    .csrf { it.disable() }
    .authorizeHttpRequests { it.anyRequest().authenticated() }
    .addFilterAfter(McpAuthenticationFilter(properties, currentUserService), SecurityContextHolderFilter::class.java)
    .build()

  @Bean
  fun webMvcStatelessServerTransport(
    @Qualifier("mcpServerJsonMapper") jsonMapper: JsonMapper,
    properties: McpProperties,
  ): WebMvcStatelessServerTransport {
    val securityValidator = DefaultServerTransportSecurityValidator.builder()
      .allowedHosts(properties.allowedHosts)
      .allowedOrigins(properties.allowedOrigins)
      .build()

    return WebMvcStatelessServerTransport.builder()
      .jsonMapper(JacksonMcpJsonMapper(jsonMapper))
      .messageEndpoint("/mcp")
      .securityValidator(securityValidator)
      .build()
  }
}

package com.application.mcp

import com.application.schema.UserRole
import com.application.security.CurrentUser
import com.application.security.UserIdPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/** Only installed in the MCP security chain; browser sessions cannot authenticate tool calls. */
class McpAuthenticationFilter(
  private val properties: McpProperties,
  private val currentUser: CurrentUser,
) : OncePerRequestFilter() {
  private val expectedToken = properties.apiToken.toByteArray(Charsets.UTF_8)

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    if (!properties.enabled) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND)
      return
    }

    if (!hasValidToken(request)) {
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
      return
    }

    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = UsernamePasswordAuthenticationToken.authenticated(
      UserIdPrincipal(properties.userId), null, emptyList(),
    )
    SecurityContextHolder.setContext(context)

    // Recheck the account on every request, including discovery and initialization.
    if (currentUser.get()?.role != UserRole.ADMIN) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN)
      return
    }

    filterChain.doFilter(request, response)
  }

  private fun hasValidToken(request: HttpServletRequest): Boolean {
    val authorization = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return false
    if (!authorization.startsWith("Bearer ", ignoreCase = true)) {
      return false
    }

    val token = authorization.substring(7).toByteArray(Charsets.UTF_8)
    return MessageDigest.isEqual(expectedToken, token)
  }
}

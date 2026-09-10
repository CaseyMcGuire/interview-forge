package com.application.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Spring Security defers CSRF token generation until something reads the token during a
 * request. Server-rendered form tags would do that read, but our pages are rendered with
 * kotlinx.html and never touch it, so on a fresh session no XSRF-TOKEN cookie is ever set
 * and the SPA's first GraphQL POST fails with a 403. Reading the token here on every
 * request makes CookieCsrfTokenRepository write the cookie on the initial page load.
 *
 * This is the filter from Spring Security's single-page-application CSRF recipe:
 * https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa
 */
class CsrfCookieFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val csrfToken = request.getAttribute("_csrf") as CsrfToken
    // Resolving the deferred token triggers the cookie write
    csrfToken.token
    filterChain.doFilter(request, response)
  }
}

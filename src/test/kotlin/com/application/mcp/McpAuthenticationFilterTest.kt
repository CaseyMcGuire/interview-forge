package com.application.mcp

import com.application.security.CurrentUserService
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class McpAuthenticationFilterTest {
  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `MCP is closed by default without inspecting credentials or users`() {
    val currentUserService = mock(CurrentUserService::class.java)
    val chain = mock(FilterChain::class.java)
    val response = MockHttpServletResponse()

    McpAuthenticationFilter(McpProperties(), currentUserService)
      .doFilter(MockHttpServletRequest("POST", "/mcp"), response, chain)

    assertEquals(404, response.status)
    verifyNoInteractions(currentUserService, chain)
  }

  @Test
  fun `a configured account that no longer exists cannot authenticate`() {
    val currentUserService = mock(CurrentUserService::class.java)
    val chain = mock(FilterChain::class.java)
    val request = MockHttpServletRequest("POST", "/mcp")
    request.addHeader("Authorization", "Bearer ${McpServerIntegrationTest.TEST_TOKEN}")
    val response = MockHttpServletResponse()
    val properties = McpProperties(apiToken = McpServerIntegrationTest.TEST_TOKEN, userId = 42)

    McpAuthenticationFilter(properties, currentUserService).doFilter(request, response, chain)

    assertEquals(403, response.status)
    verify(currentUserService).get()
    verifyNoInteractions(chain)
  }

  @Test
  fun `MCP enables automatically when both credentials are configured`() {
    assertFalse(McpProperties().enabled)
    assertTrue(McpProperties(apiToken = McpServerIntegrationTest.TEST_TOKEN, userId = 42).enabled)
  }

  @Test
  fun `partial or invalid MCP credentials fail configuration`() {
    assertThrows(IllegalArgumentException::class.java) { McpProperties(userId = 42) }
    assertThrows(IllegalArgumentException::class.java) {
      McpProperties(apiToken = McpServerIntegrationTest.TEST_TOKEN)
    }
    assertThrows(IllegalArgumentException::class.java) {
      McpProperties(apiToken = "short", userId = 42)
    }
  }
}

package com.application.mcp

import com.application.services.ProblemInputException
import entkt.runtime.result.EntValidationException
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/** Gives tool failures both the MCP error flag and field errors the caller can correct. */
@Component
class McpToolResults(private val mapper: ObjectMapper) {
  fun withValidationErrors(action: () -> CallToolResult): CallToolResult = try {
    action()
  } catch (exception: EntValidationException) {
    failure(exception.violations.map { McpToolError(it.field, it.message) })
  } catch (exception: ProblemInputException) {
    failure(exception.message, exception.field)
  } catch (exception: IllegalArgumentException) {
    failure(exception.message ?: "Invalid tool input")
  }

  fun success(content: Any): CallToolResult = response(content, isError = false)

  fun failure(message: String, field: String? = null): CallToolResult =
    failure(listOf(McpToolError(field, message)))

  private fun failure(errors: List<McpToolError>): CallToolResult =
    response(McpToolFailure(errors), isError = true)

  private fun response(content: Any, isError: Boolean): CallToolResult = CallToolResult.builder()
    .structuredContent(content)
    .addTextContent(mapper.writeValueAsString(content))
    .isError(isError)
    .build()
}

data class McpToolError(val field: String?, val message: String)

data class McpToolFailure(val errors: List<McpToolError>)

package com.application.services

import com.application.ent.EntClient
import com.application.execution.CodeExecutionResult
import com.application.execution.CodeExecutionService
import com.application.execution.CodeExecutionSettings
import com.application.execution.ExecutionStartup
import com.application.execution.ProgramStatus
import com.application.execution.TestSuiteProtocol
import com.application.security.CurrentUserService
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.visibleOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

/** Computes one expected answer without creating a test case, submission, or grading job. */
@Service
class TestCaseExpectedOutputService(
  private val entClient: EntClient,
  private val currentUserService: CurrentUserService,
  private val settingsService: CodeExecutionSettingsService,
  private val executionService: CodeExecutionService,
  private val startup: ExecutionStartup,
) {
  private val publicContext = ViewerContext(Viewer.Anonymous)
  private val inputMapper = JsonMapper.builder(
    JsonFactory.builder()
      .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(20_000).build())
      .build(),
  )
    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
    .build()

  fun generateExpectedOutput(problemLanguageId: Long?, inputJson: String): TestCaseExpectedOutputOutcome {
    currentUserService.requireAdmin()
    val configurationId = problemLanguageId
      ?: throw ProblemInputException("problemLanguageId", "Provide a valid problem-language ID")
    val input = parseTestInput(inputJson)

    val configuration = entClient.problemLanguages.findById(publicContext, configurationId)
      .visibleOrNull()
      .getOrThrow()
      ?: return TestCaseExpectedOutputOutcome.NotFound

    val settings = settingsService.findReferenceSolutionSettings(configuration.id)
      ?: return TestCaseExpectedOutputOutcome.Unavailable

    if (!executionService.isAvailable(settings.runtime) || !startup.executionReady()) {
      return TestCaseExpectedOutputOutcome.Unavailable
    }

    val result = executeReferenceSolution(settings, input)
    return readExpectedOutput(input, result)
  }

  private fun parseTestInput(value: String): JsonElement {
    val parsed = try {
      inputMapper.readTree(value)
    } catch (_: JacksonException) {
      throw ProblemInputException("inputJson", "Provide valid JSON")
    }

    if (parsed == null || parsed.isMissingNode) {
      throw ProblemInputException("inputJson", "Provide valid JSON")
    }

    // Preserve the original numeric representation after checking strict JSON syntax.
    val input = Json.parseToJsonElement(value)
    if (input.toString().length > 20_000) {
      throw ProblemInputException("inputJson", "Input must be at most 20,000 characters")
    }

    return input
  }

  private fun executeReferenceSolution(settings: CodeExecutionSettings, input: JsonElement): CodeExecutionResult = try {
    executionService.executeCode(
      executionId = "test-case-reference-${UUID.randomUUID()}",
      runtime = settings.runtime,
      program = settings.program,
      inputs = listOf(input),
      timeLimitMs = settings.timeLimitMs,
      memoryLimitMb = settings.memoryLimitMb,
    )
  } catch (interruption: InterruptedException) {
    Thread.currentThread().interrupt()
    throw interruption
  }

  private fun readExpectedOutput(input: JsonElement, result: CodeExecutionResult): TestCaseExpectedOutputOutcome {
    if (result is CodeExecutionResult.CompilationFailed) {
      return TestCaseExpectedOutputOutcome.ReferenceFailed("The reference solution failed to compile.")
    }

    check(result is CodeExecutionResult.Completed)
    if (result.status != ProgramStatus.SUCCEEDED) {
      return referenceExecutionFailure(result.status)
    }

    val testCase = result.caseResults.singleOrNull()
      ?: return TestCaseExpectedOutputOutcome.ReferenceFailed("The reference solution did not return one answer.")
    check(testCase.inputJson == input) { "Reference result does not match its input" }

    if (testCase.status != ProgramStatus.SUCCEEDED) {
      return referenceExecutionFailure(testCase.status)
    }

    val output = testCase.outputJson
      ?: return TestCaseExpectedOutputOutcome.ReferenceFailed("The reference solution did not return valid JSON.")

    if (output.toString().toByteArray(Charsets.UTF_8).size > TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES) {
      return referenceExecutionFailure(ProgramStatus.OUTPUT_LIMIT_EXCEEDED)
    }

    return TestCaseExpectedOutputOutcome.Success(output)
  }

  private fun referenceExecutionFailure(status: ProgramStatus): TestCaseExpectedOutputOutcome.ReferenceFailed {
    val message = when (status) {
      ProgramStatus.FAILED -> "The reference solution stopped with an error."
      ProgramStatus.TIME_LIMIT_EXCEEDED -> "The reference solution exceeded the time limit."
      ProgramStatus.MEMORY_LIMIT_EXCEEDED -> "The reference solution exceeded the memory limit."
      ProgramStatus.OUTPUT_LIMIT_EXCEEDED -> "The reference solution exceeded the output limit."
      ProgramStatus.SUCCEEDED -> error("A successful execution has no failure message")
    }

    return TestCaseExpectedOutputOutcome.ReferenceFailed(message)
  }
}

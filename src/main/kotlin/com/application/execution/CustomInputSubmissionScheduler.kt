package com.application.execution

import com.application.ent.CustomInputSubmission
import com.application.schema.CustomInputSubmissionOutcome
import com.application.services.CodeExecutionSettingsService
import com.application.services.CustomInputSubmissionService
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Prepares expected answers for queued custom input submissions and creates ready grading jobs. */
@Component
class CustomInputSubmissionScheduler(
  private val customInputSubmissionService: CustomInputSubmissionService,
  private val settingsService: CodeExecutionSettingsService,
  private val executionService: CodeExecutionService,
  private val startup: ExecutionStartup,
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  // Retain this claim if its final write fails; recovery must not touch the grading scheduler's work.
  private var unfinishedCustomInputSubmissionId: Long? = null

  @Scheduled(fixedDelay = 1_000)
  @Synchronized
  fun prepareQueuedCustomInputSubmissions() {
    try {
      if (!startup.workersReady()) {
        return
      }

      recoverPreviousPreparation()
      prepareNextCustomInputSubmission()
    } catch (interruption: InterruptedException) {
      Thread.currentThread().interrupt()
      throw interruption
    } catch (exception: Exception) {
      logger.error("Custom test suite scheduler could not complete its database or runtime operation", exception)
    }
  }

  private fun recoverPreviousPreparation() {
    val id = unfinishedCustomInputSubmissionId ?: return
    customInputSubmissionService.finishInterruptedCustomPreparation(id)
    unfinishedCustomInputSubmissionId = null
  }

  private fun prepareNextCustomInputSubmission() {
    val customInputSubmission = customInputSubmissionService.claimNextQueuedCustomInputSubmission() ?: return

    unfinishedCustomInputSubmissionId = customInputSubmission.id

    val expectedOutputs = try {
      generateExpectedOutputs(customInputSubmission)
    } catch (interruption: InterruptedException) {
      finishInterruptedCustomInputSubmission(customInputSubmission.id)
      throw interruption
    } catch (exception: Exception) {
      logger.error(
        "Reference preparation failed for custom input submission {} ({})",
        customInputSubmission.id,
        exception.javaClass.simpleName,
      )
      customInputSubmissionService.finishCustomInputSubmission(
        customInputSubmission.id,
        CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR),
      )
      unfinishedCustomInputSubmissionId = null
      return
    }

    if (expectedOutputs == null) {
      customInputSubmissionService.finishCustomInputSubmission(
        customInputSubmission.id,
        CustomInputSubmissionResult(CustomInputSubmissionOutcome.REFERENCE_SOLUTION_FAILED),
      )
      unfinishedCustomInputSubmissionId = null
      return
    }

    // Do not retry an uncertain commit: recovery distinguishes a ready job from an interrupted preparation.
    customInputSubmissionService.saveExpectedOutputsAndEnqueueGradingJob(customInputSubmission.id, expectedOutputs)
    unfinishedCustomInputSubmissionId = null
  }

  private fun generateExpectedOutputs(customInputSubmission: CustomInputSubmission): List<JsonElement>? {
    val inputs = customInputSubmissionService.loadCustomInputs(customInputSubmission.id)
    val settings = settingsService.loadReferenceSolutionSettings(customInputSubmission.problemLanguageId)
    val result = executionService.executeCode(
      executionId = "custom-reference-${customInputSubmission.id}",
      runtime = settings.runtime,
      program = settings.program,
      inputs = inputs,
      timeLimitMs = settings.timeLimitMs,
      memoryLimitMb = settings.memoryLimitMb,
    )

    return readExpectedOutputs(inputs, result)
  }

  private fun readExpectedOutputs(inputs: List<JsonElement>, result: CodeExecutionResult): List<JsonElement>? {
    if (result !is CodeExecutionResult.Completed || result.status != ProgramStatus.SUCCEEDED) {
      return null
    }

    if (result.caseResults.size != inputs.size) {
      return null
    }

    val outputs = mutableListOf<JsonElement>()
    for ((index, caseResult) in result.caseResults.withIndex()) {
      check(caseResult.inputJson == inputs[index]) { "Reference result does not match its input" }

      if (caseResult.status != ProgramStatus.SUCCEEDED) {
        return null
      }

      // Kotlin null means no answer; JsonNull is a valid prepared answer.
      val output = caseResult.outputJson ?: return null

      if (output.toString().toByteArray(Charsets.UTF_8).size > TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES) {
        return null
      }

      outputs += output
    }

    return outputs
  }

  private fun finishInterruptedCustomInputSubmission(customInputSubmissionId: Long) {
    try {
      customInputSubmissionService.finishCustomInputSubmission(
        customInputSubmissionId,
        CustomInputSubmissionResult(CustomInputSubmissionOutcome.INTERNAL_ERROR),
      )
      unfinishedCustomInputSubmissionId = null
    } finally {
      Thread.currentThread().interrupt()
    }
  }
}

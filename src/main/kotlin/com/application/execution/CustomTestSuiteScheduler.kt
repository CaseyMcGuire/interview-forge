package com.application.execution

import com.application.ent.CustomTestSuiteRun
import com.application.schema.CustomTestSuiteRunOutcome
import com.application.services.CodeExecutionSettingsService
import com.application.services.CustomTestSuiteRunService
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Prepares expected answers for queued custom runs and creates ready grading jobs. */
@Component
class CustomTestSuiteScheduler(
  private val customRunService: CustomTestSuiteRunService,
  private val settingsService: CodeExecutionSettingsService,
  private val executionService: CodeExecutionService,
  private val startup: ExecutionStartup,
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  // Retain this claim if its final write fails; recovery must not touch the grading scheduler's work.
  private var unfinishedRunId: Long? = null

  @Scheduled(fixedDelay = 1_000)
  @Synchronized
  fun prepareQueuedCustomRuns() {
    try {
      if (!startup.workersReady()) {
        return
      }

      recoverPreviousPreparation()
      prepareNextCustomRun()
    } catch (interruption: InterruptedException) {
      Thread.currentThread().interrupt()
      throw interruption
    } catch (exception: Exception) {
      logger.error("Custom test suite scheduler could not complete its database or runtime operation", exception)
    }
  }

  private fun recoverPreviousPreparation() {
    val id = unfinishedRunId ?: return
    customRunService.finishInterruptedCustomPreparation(id)
    unfinishedRunId = null
  }

  private fun prepareNextCustomRun() {
    val run = customRunService.claimNextQueuedCustomRun() ?: return

    unfinishedRunId = run.id

    val expectedOutputs = try {
      generateExpectedOutputs(run)
    } catch (interruption: InterruptedException) {
      finishInterruptedCustomRun(run.id)
      throw interruption
    } catch (exception: Exception) {
      logger.error("Reference preparation failed for custom run {} ({})", run.id, exception.javaClass.simpleName)
      customRunService.finishCustomTestSuiteRun(
        run.id,
        CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR),
      )
      unfinishedRunId = null
      return
    }

    if (expectedOutputs == null) {
      customRunService.finishCustomTestSuiteRun(
        run.id,
        CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.REFERENCE_SOLUTION_FAILED),
      )
      unfinishedRunId = null
      return
    }

    // Do not retry an uncertain commit: recovery distinguishes a ready job from an interrupted preparation.
    customRunService.saveExpectedOutputsAndEnqueueGradingJob(run.id, expectedOutputs)
    unfinishedRunId = null
  }

  private fun generateExpectedOutputs(run: CustomTestSuiteRun): List<JsonElement>? {
    val inputs = customRunService.loadCustomInputs(run.id)
    val settings = settingsService.loadReferenceSolutionSettings(run.problemLanguageId)
    val result = executionService.executeCode(
      executionId = "custom-reference-${run.id}",
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

  private fun finishInterruptedCustomRun(runId: Long) {
    try {
      customRunService.finishCustomTestSuiteRun(
        runId,
        CustomTestSuiteRunResult(CustomTestSuiteRunOutcome.INTERNAL_ERROR),
      )
      unfinishedRunId = null
    } finally {
      Thread.currentThread().interrupt()
    }
  }
}

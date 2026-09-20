package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.GradingJob
import com.application.services.GradingJobService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Processes one persisted job at a time, keeping compilation and execution outside database transactions. */
@Component
class GradingJobScheduler(
  private val gradingJobService: GradingJobService,
  private val codeGrader: CodeGrader,
  private val executionService: CodeExecutionService,
  private val properties: ExecutionProperties,
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var initialized = false

  @Volatile
  private var applicationReady = false

  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    applicationReady = true
  }

  @Scheduled(fixedDelay = 1_000)
  @Synchronized
  fun processQueuedGradingJobs() {
    if (!applicationReady || !properties.workerEnabled || properties.runtimes.isEmpty()) {
      return
    }

    try {
      if (properties.runtimes.values.none(executionService::isAvailable)) {
        return
      }

      recoverInterruptedGradingJobs()

      val job = gradingJobService.claimNextQueuedGradingJob() ?: return
      val result = gradeClaimedJob(job)

      // A failed final write may have committed. Recover remaining RUNNING jobs on the next tick; never rerun them.
      gradingJobService.finishGradingJob(job.id, result)
    } catch (interruption: InterruptedException) {
      Thread.currentThread().interrupt()
      throw interruption
    } catch (exception: Exception) {
      logger.error("Grading scheduler could not complete its database or runtime operation", exception)
    }
  }

  private fun recoverInterruptedGradingJobs() {
    if (!initialized) {
      executionService.cleanUpInterruptedExecutions()
      initialized = true
    }

    gradingJobService.finishInterruptedGradingJobs()
  }

  private fun gradeClaimedJob(job: GradingJob): GradingResult {
    try {
      val settings = gradingJobService.loadExecutionSettings(job)

      return codeGrader.gradeCode(
        executionId = "grading-job-${job.id}",
        runtime = settings.runtime,
        program = settings.program,
        cases = job.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
        timeLimitMs = settings.timeLimitMs,
        memoryLimitMb = settings.memoryLimitMb,
      )
    } catch (interruption: InterruptedException) {
      finishInterruptedGradingJob(job.id)
      throw interruption
    } catch (exception: Exception) {
      // Exception messages and raw diagnostics can contain private driver code or hidden inputs.
      logger.error("Grading failed for job {} ({})", job.id, exception.javaClass.simpleName)
      return GradingResult(
        outcome = GradingOutcome.INTERNAL_ERROR,
        caseResults = job.cases.map { TestCaseGradingResult(TestCaseOutcome.NOT_RUN) },
      )
    }
  }

  private fun finishInterruptedGradingJob(jobId: Long) {
    try {
      gradingJobService.failGradingJob(jobId)
    } finally {
      Thread.currentThread().interrupt()
    }
  }
}

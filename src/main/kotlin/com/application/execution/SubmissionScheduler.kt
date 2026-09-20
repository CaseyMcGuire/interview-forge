package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.Submission
import com.application.schema.SubmissionVerdict
import com.application.services.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Starts queue processing after application startup and keeps transient infrastructure failures retryable. */
@Component
class SubmissionScheduler(
  private val submissionService: SubmissionService,
  private val submissionRunner: SubmissionRunner,
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
  fun processQueuedSubmissions() {
    if (!applicationReady || !properties.workerEnabled || properties.runtimes.isEmpty()) {
      return
    }

    try {
      if (properties.runtimes.values.none(executionService::isAvailable)) {
        return
      }

      recoverInterruptedSubmissions()

      val submission = submissionService.claimNextQueuedSubmission() ?: return
      val result = runClaimedSubmission(submission)

      // Do not retry a final write: a lost connection can leave its commit outcome unknown.
      submissionService.finishSubmission(submission.id, result)
    } catch (interruption: InterruptedException) {
      Thread.currentThread().interrupt()
      throw interruption
    } catch (exception: Exception) {
      // A later scheduled call can retry recovery; individual submissions are not automatically rerun.
      logger.error("Submission scheduler could not complete its database or runtime operation", exception)
    }
  }

  private fun recoverInterruptedSubmissions() {
    if (!initialized) {
      executionService.cleanUpInterruptedExecutions()
      initialized = true
    }

    submissionService.finishInterruptedSubmissions()
  }

  private fun runClaimedSubmission(submission: Submission): SubmissionExecutionResult {
    try {
      return submissionRunner.runSubmission(submission)
    } catch (interruption: InterruptedException) {
      finishInterruptedSubmission(submission.id)
      throw interruption
    }
  }

  private fun finishInterruptedSubmission(submissionId: Long) {
    try {
      submissionService.finishSubmission(submissionId, SubmissionExecutionResult(SubmissionVerdict.INTERNAL_ERROR))
    } finally {
      Thread.currentThread().interrupt()
    }
  }
}

package com.application.execution

import com.application.config.ExecutionProperties
import com.application.services.CustomInputSubmissionService
import com.application.services.GradingJobService
import org.springframework.stereotype.Component

/** Finishes startup recovery before either scheduler can claim work. One app instance may own execution. */
@Component
class ExecutionStartup(
  private val gradingJobService: GradingJobService,
  private val customInputSubmissionService: CustomInputSubmissionService,
  private val executionService: CodeExecutionService,
  private val properties: ExecutionProperties,
  private val workerReadiness: ScheduledWorkerReadiness,
) {
  private var recovered = false

  /** Gates workers on startup recovery, retrying failures without repeating recovery after success. */
  @Synchronized
  fun workersReady(): Boolean {
    if (!workerReadiness.isReady()) {
      return false
    }

    if (properties.runtimes.values.none(executionService::isAvailable)) {
      return false
    }

    if (!recovered) {
      executionService.cleanUpInterruptedExecutions()
      gradingJobService.finishInterruptedGradingJobs()
      customInputSubmissionService.finishInterruptedCustomPreparations()
      recovered = true
    }

    return true
  }
}

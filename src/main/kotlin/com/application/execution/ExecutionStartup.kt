package com.application.execution

import com.application.config.ExecutionProperties
import com.application.services.CustomTestSuiteRunService
import com.application.services.GradingJobService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/** Finishes startup recovery before either scheduler can claim work. One app instance may own execution. */
@Component
class ExecutionStartup(
  private val gradingJobService: GradingJobService,
  private val customRunService: CustomTestSuiteRunService,
  private val executionService: CodeExecutionService,
  private val properties: ExecutionProperties,
) {
  private var recovered = false

  @Volatile
  private var applicationReady = false

  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    applicationReady = true
  }

  /** Gates workers on startup recovery, retrying failures without repeating recovery after success. */
  @Synchronized
  fun workersReady(): Boolean {
    if (!applicationReady || !properties.workerEnabled || properties.runtimes.isEmpty()) {
      return false
    }

    if (properties.runtimes.values.none(executionService::isAvailable)) {
      return false
    }

    if (!recovered) {
      executionService.cleanUpInterruptedExecutions()
      gradingJobService.finishInterruptedGradingJobs()
      customRunService.finishInterruptedCustomPreparations()
      recovered = true
    }

    return true
  }
}

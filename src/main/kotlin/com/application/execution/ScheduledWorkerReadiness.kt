package com.application.execution

import com.application.config.ExecutionProperties
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.ReadinessState
import org.springframework.stereotype.Component

/** Shared startup and enablement check for preparation, grading, and expiration cleanup. */
@Component
class ScheduledWorkerReadiness(
  private val applicationAvailability: ApplicationAvailability,
  private val properties: ExecutionProperties,
) {
  fun isReady(): Boolean =
    applicationAvailability.readinessState == ReadinessState.ACCEPTING_TRAFFIC && properties.workerEnabled
}

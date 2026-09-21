package com.application.execution

import com.application.services.CustomInputSubmissionService
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/** Removes expired results independently of Docker availability and the grading queues. */
@Component
class CustomInputSubmissionCleanupScheduler(
  private val customInputSubmissionService: CustomInputSubmissionService,
  workerReadiness: ScheduledWorkerReadiness,
) : AbstractScheduler(Duration.ofMinutes(1), workerReadiness::isReady) {
  override fun executeTask() {
    customInputSubmissionService.deleteExpiredCustomInputSubmissions(Instant.now())
  }
}

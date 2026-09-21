package com.application.execution

import com.application.config.ExecutionProperties
import com.application.services.CustomInputSubmissionService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.ReadinessState
import java.time.Instant

class CustomInputSubmissionCleanupSchedulerTest {
  private val applicationAvailability = mock(ApplicationAvailability::class.java)

  @Test
  fun `cleanup waits for application readiness and respects the worker switch`() {
    val service = mock(CustomInputSubmissionService::class.java)
    val starting = createScheduler(service)
    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.REFUSING_TRAFFIC)
    starting.runScheduledTask()

    val disabled = createScheduler(service, ExecutionProperties(workerEnabled = false))
    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.ACCEPTING_TRAFFIC)
    disabled.runScheduledTask()

    verifyNoInteractions(service)
  }

  @Test
  fun `cleanup runs without a configured runtime and uses the current expiration cutoff`() {
    val service = mock(CustomInputSubmissionService::class.java)
    val scheduler = createScheduler(service, ExecutionProperties(runtimes = emptyMap()))
    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.ACCEPTING_TRAFFIC)
    val before = Instant.now()

    scheduler.runScheduledTask()

    val call = mockingDetails(service).invocations.single()
    val cutoff = call.arguments[0] as Instant
    assertTrue(cutoff >= before && cutoff <= Instant.now())
    assertEquals(100, call.arguments[1])

    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.REFUSING_TRAFFIC)
    scheduler.runScheduledTask()
    assertEquals(1, mockingDetails(service).invocations.size)
  }

  @Test
  fun `a database failure does not prevent the next cleanup tick`() {
    var attempts = 0
    val service = mock(CustomInputSubmissionService::class.java) {
      attempts++
      if (attempts == 1) {
        error("Database unavailable")
      }
      0
    }
    val scheduler = createScheduler(service)
    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.ACCEPTING_TRAFFIC)

    scheduler.runScheduledTask()
    scheduler.runScheduledTask()

    assertEquals(2, attempts)
  }

  private fun createScheduler(
    service: CustomInputSubmissionService,
    properties: ExecutionProperties = ExecutionProperties(),
  ): CustomInputSubmissionCleanupScheduler {
    val workerReadiness = ScheduledWorkerReadiness(applicationAvailability, properties)
    return CustomInputSubmissionCleanupScheduler(service, workerReadiness)
  }
}

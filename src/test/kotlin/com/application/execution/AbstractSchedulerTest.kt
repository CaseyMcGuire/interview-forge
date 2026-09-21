package com.application.execution

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.time.Duration

class AbstractSchedulerTest {
  @Test
  fun `registration defers readiness and work until the scheduled task runs`() {
    var readinessChecks = 0
    var executions = 0
    val scheduler = object : AbstractScheduler(
      delay = Duration.ofMinutes(1),
      workersReady = {
        readinessChecks++
        true
      },
    ) {
      override fun executeTask() {
        executions++
      }
    }
    val registrar = ScheduledTaskRegistrar()

    scheduler.configureTasks(registrar)

    val task = registrar.fixedDelayTaskList.single()
    assertEquals(Duration.ofMinutes(1), task.intervalDuration)
    assertEquals(0, readinessChecks)
    assertEquals(0, executions)

    task.runnable.run()

    assertEquals(1, readinessChecks)
    assertEquals(1, executions)
  }

  @ParameterizedTest
  @ValueSource(longs = [0, -1])
  fun `an invalid interval fails during construction`(delayMs: Long) {
    assertThrows(IllegalArgumentException::class.java) {
      object : AbstractScheduler(Duration.ofMillis(delayMs), { true }) {
        override fun executeTask() = Unit
      }
    }
  }
}

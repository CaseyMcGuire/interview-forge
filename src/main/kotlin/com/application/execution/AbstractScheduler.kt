package com.application.execution

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.time.Duration

/** Registers one recurring task with Spring, prevents overlapping runs, and handles readiness and failures. */
abstract class AbstractScheduler(
  private val delay: Duration,
  private val workersReady: () -> Boolean,
) : SchedulingConfigurer {
  private val logger = LoggerFactory.getLogger(javaClass)

  init {
    require(!delay.isNegative && !delay.isZero) { "Scheduler delay must be positive" }
  }

  // Spring calls this after bean construction and owns the scheduled task's shutdown.
  final override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
    taskRegistrar.addFixedDelayTask(::runScheduledTask, delay)
  }

  @Synchronized
  fun runScheduledTask() {
    try {
      if (!workersReady()) {
        return
      }

      executeTask()
    } catch (interruption: InterruptedException) {
      Thread.currentThread().interrupt()
      throw interruption
    } catch (exception: Exception) {
      logger.error("Scheduled task could not complete", exception)
    }
  }

  protected abstract fun executeTask()
}

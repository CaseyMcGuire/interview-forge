package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.GradingJob
import com.application.services.GradingJobService
import com.application.services.CustomInputSubmissionService
import com.application.services.CodeExecutionSettingsService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.*
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.ReadinessState

class GradingSchedulerTest {
  private lateinit var startup: ExecutionStartup
  private val applicationAvailability = mock(ApplicationAvailability::class.java)
  private val gradingJobService = mock(GradingJobService::class.java)
  private val customInputSubmissionService = mock(CustomInputSubmissionService::class.java)
  private val settingsService = mock(CodeExecutionSettingsService::class.java)
  private val codeGrader = mock(CodeGrader::class.java)
  private val executor = mock(CodeExecutionService::class.java)
  private val program = PreparedProgram(emptyMap(), null, listOf("run"))
  private val settings = CodeExecutionSettings("runtime", program, 1_000, 128)
  private val properties = ExecutionProperties(runtimes = mapOf("kotlin" to "runtime"))

  @Test
  fun `workers follow application readiness changes`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.REFUSING_TRAFFIC)

    scheduler.runScheduledTask()
    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader, executor)

    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.ACCEPTING_TRAFFIC)
    scheduler.runScheduledTask()
    verify(gradingJobService).claimNextQueuedGradingJob()

    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.REFUSING_TRAFFIC)
    scheduler.runScheduledTask()
    verify(gradingJobService, times(1)).claimNextQueuedGradingJob()
  }

  @Test
  fun `a disabled worker leaves the queue untouched`() {
    val scheduler = createScheduler(properties.copy(workerEnabled = false))

    scheduler.runScheduledTask()

    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader, executor)
  }

  @Test
  fun `queue processing resumes when the runtime becomes available`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(false, true)

    scheduler.runScheduledTask()
    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader)

    scheduler.runScheduledTask()
    verify(gradingJobService).claimNextQueuedGradingJob()
    verifyNoInteractions(codeGrader)
  }

  @Test
  fun `failed recovery does not stop later queue processing`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    doThrow(IllegalStateException("cleanup failed")).doNothing().`when`(executor).cleanUpInterruptedExecutions()

    scheduler.runScheduledTask()
    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader)

    scheduler.runScheduledTask()

    verify(executor, times(2)).cleanUpInterruptedExecutions()
    verify(gradingJobService).finishInterruptedGradingJobs()
    verify(gradingJobService).claimNextQueuedGradingJob()
    verifyNoInteractions(codeGrader)
  }

  @Test
  fun `the scheduler recovers claims runs and saves in order`() {
    val scheduler = createScheduler()
    val job = mock(GradingJob::class.java)
    val result = GradingResult(GradingOutcome.PASSED, emptyList())
    `when`(job.id).thenReturn(123L)
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    `when`(gradingJobService.claimNextQueuedGradingJob()).thenReturn(job)
    `when`(job.cases).thenReturn(emptyList())
    `when`(job.problemLanguageId).thenReturn(12L)
    `when`(job.sourceCode).thenReturn("source")
    `when`(settingsService.loadSubmittedCodeSettings(12L, "source")).thenReturn(settings)
    `when`(codeGrader.gradeCode("grading-job-123", "runtime", program, emptyList(), 1_000, 128)).thenReturn(result)

    scheduler.runScheduledTask()

    val order = inOrder(executor, gradingJobService, customInputSubmissionService, settingsService, codeGrader)
    order.verify(executor).cleanUpInterruptedExecutions()
    order.verify(gradingJobService).finishInterruptedGradingJobs()
    order.verify(customInputSubmissionService).finishInterruptedCustomPreparations()
    order.verify(gradingJobService).claimNextQueuedGradingJob()
    order.verify(settingsService).loadSubmittedCodeSettings(12L, "source")
    order.verify(codeGrader).gradeCode("grading-job-123", "runtime", program, emptyList(), 1_000, 128)
    order.verify(gradingJobService).finishGradingJob(job.id, result)
  }

  @Test
  fun `a failed final write triggers recovery on the next tick without rerunning the job`() {
    val scheduler = createScheduler()
    val job = mock(GradingJob::class.java)
    val result = GradingResult(GradingOutcome.PASSED, emptyList())
    `when`(job.id).thenReturn(123L)
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    `when`(gradingJobService.claimNextQueuedGradingJob()).thenReturn(job, null)
    `when`(job.cases).thenReturn(emptyList())
    `when`(job.problemLanguageId).thenReturn(12L)
    `when`(job.sourceCode).thenReturn("source")
    `when`(settingsService.loadSubmittedCodeSettings(12L, "source")).thenReturn(settings)
    `when`(codeGrader.gradeCode("grading-job-123", "runtime", program, emptyList(), 1_000, 128)).thenReturn(result)
    doThrow(IllegalStateException("connection lost")).`when`(gradingJobService).finishGradingJob(123L, result)

    scheduler.runScheduledTask()
    scheduler.runScheduledTask()

    verify(executor).cleanUpInterruptedExecutions()
    verify(gradingJobService).finishInterruptedGradingJobs()
    verify(gradingJobService).failGradingJob(123L)
    verify(codeGrader).gradeCode("grading-job-123", "runtime", program, emptyList(), 1_000, 128)
    verify(gradingJobService).finishGradingJob(job.id, result)
  }

  @Test
  fun `both schedulers finish startup recovery before claiming and never repeat it`() {
    val gradingScheduler = createScheduler()
    val customScheduler = CustomInputSubmissionScheduler(customInputSubmissionService, settingsService, executor, startup)
    `when`(executor.isAvailable("runtime")).thenReturn(true)

    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()
    customScheduler.runScheduledTask()
    gradingScheduler.runScheduledTask()

    val order = inOrder(executor, gradingJobService, customInputSubmissionService)
    order.verify(executor).cleanUpInterruptedExecutions()
    order.verify(gradingJobService).finishInterruptedGradingJobs()
    order.verify(customInputSubmissionService).finishInterruptedCustomPreparations()
    order.verify(customInputSubmissionService).claimNextQueuedCustomInputSubmission()
    order.verify(gradingJobService).claimNextQueuedGradingJob()
    verify(executor).cleanUpInterruptedExecutions()
    verify(gradingJobService).finishInterruptedGradingJobs()
    verify(customInputSubmissionService).finishInterruptedCustomPreparations()
  }

  @Test
  fun `a direct execution request recovers before workers and recovery is not repeated`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(true)

    assertTrue(startup.executionReady())
    scheduler.runScheduledTask()

    val order = inOrder(executor, gradingJobService, customInputSubmissionService)
    order.verify(executor).cleanUpInterruptedExecutions()
    order.verify(gradingJobService).finishInterruptedGradingJobs()
    order.verify(customInputSubmissionService).finishInterruptedCustomPreparations()
    order.verify(gradingJobService).claimNextQueuedGradingJob()
    verify(executor, times(1)).cleanUpInterruptedExecutions()
  }

  private fun createScheduler(properties: ExecutionProperties = this.properties): GradingScheduler {
    `when`(applicationAvailability.readinessState).thenReturn(ReadinessState.ACCEPTING_TRAFFIC)
    val workerReadiness = ScheduledWorkerReadiness(applicationAvailability, properties)
    startup = ExecutionStartup(gradingJobService, customInputSubmissionService, executor, properties, workerReadiness)
    return GradingScheduler(gradingJobService, settingsService, codeGrader, startup)
  }
}

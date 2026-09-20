package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.GradingJob
import com.application.services.GradingJobService
import com.application.services.CustomInputSubmissionService
import com.application.services.CodeExecutionSettingsService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class GradingSchedulerTest {
  private lateinit var startup: ExecutionStartup
  private val gradingJobService = mock(GradingJobService::class.java)
  private val customInputSubmissionService = mock(CustomInputSubmissionService::class.java)
  private val settingsService = mock(CodeExecutionSettingsService::class.java)
  private val codeGrader = mock(CodeGrader::class.java)
  private val executor = mock(CodeExecutionService::class.java)
  private val program = PreparedProgram(emptyMap(), null, listOf("run"))
  private val settings = CodeExecutionSettings("runtime", program, 1_000, 128)
  private val properties = ExecutionProperties(runtimes = mapOf("kotlin" to "runtime"))

  @Test
  fun `a disabled worker leaves the queue untouched`() {
    val scheduler = createScheduler(properties.copy(workerEnabled = false))

    startup.onApplicationReady()
    scheduler.processQueuedGradingJobs()

    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader, executor)
  }

  @Test
  fun `queue processing resumes when the runtime becomes available`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(false, true)
    startup.onApplicationReady()

    scheduler.processQueuedGradingJobs()
    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader)

    scheduler.processQueuedGradingJobs()
    verify(gradingJobService).claimNextQueuedGradingJob()
    verifyNoInteractions(codeGrader)
  }

  @Test
  fun `failed recovery does not stop later queue processing`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    doThrow(IllegalStateException("cleanup failed")).doNothing().`when`(executor).cleanUpInterruptedExecutions()
    startup.onApplicationReady()

    scheduler.processQueuedGradingJobs()
    verifyNoInteractions(gradingJobService, customInputSubmissionService, settingsService, codeGrader)

    scheduler.processQueuedGradingJobs()

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
    startup.onApplicationReady()

    scheduler.processQueuedGradingJobs()

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
    startup.onApplicationReady()

    scheduler.processQueuedGradingJobs()
    scheduler.processQueuedGradingJobs()

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
    startup.onApplicationReady()

    customScheduler.prepareQueuedCustomInputSubmissions()
    gradingScheduler.processQueuedGradingJobs()
    customScheduler.prepareQueuedCustomInputSubmissions()
    gradingScheduler.processQueuedGradingJobs()

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

  private fun createScheduler(properties: ExecutionProperties = this.properties): GradingScheduler {
    startup = ExecutionStartup(gradingJobService, customInputSubmissionService, executor, properties)
    return GradingScheduler(gradingJobService, settingsService, codeGrader, startup)
  }
}

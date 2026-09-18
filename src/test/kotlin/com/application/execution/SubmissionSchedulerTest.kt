package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.Submission
import com.application.schema.SubmissionVerdict
import com.application.services.SubmissionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class SubmissionSchedulerTest {
  private val submissionService = mock(SubmissionService::class.java)
  private val submissionRunner = mock(SubmissionRunner::class.java)
  private val executor = mock(ProgramExecutor::class.java)
  private val properties = ExecutionProperties(runtimes = mapOf("kotlin" to "runtime"))

  @Test
  fun `a disabled worker leaves the queue untouched`() {
    val scheduler = createScheduler(properties.copy(workerEnabled = false))

    scheduler.onApplicationReady()
    scheduler.processQueuedSubmissions()

    verifyNoInteractions(submissionService, submissionRunner, executor)
  }

  @Test
  fun `queue processing resumes when the runtime becomes available`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(false, true)
    scheduler.onApplicationReady()

    scheduler.processQueuedSubmissions()
    verifyNoInteractions(submissionService, submissionRunner)

    scheduler.processQueuedSubmissions()
    verify(submissionService).claimNextQueuedSubmission()
    verifyNoInteractions(submissionRunner)
  }

  @Test
  fun `failed recovery does not stop later queue processing`() {
    val scheduler = createScheduler()
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    doThrow(IllegalStateException("cleanup failed")).doNothing().`when`(executor).cleanUpInterruptedExecutions()
    scheduler.onApplicationReady()

    scheduler.processQueuedSubmissions()
    verifyNoInteractions(submissionService, submissionRunner)

    scheduler.processQueuedSubmissions()

    verify(executor, times(2)).cleanUpInterruptedExecutions()
    verify(submissionService).finishInterruptedSubmissions()
    verify(submissionService).claimNextQueuedSubmission()
    verifyNoInteractions(submissionRunner)
  }

  @Test
  fun `the scheduler recovers claims runs and saves in order`() {
    val scheduler = createScheduler()
    val submission = mock(Submission::class.java)
    val result = SubmissionExecutionResult(SubmissionVerdict.ACCEPTED)
    `when`(submission.id).thenReturn(123L)
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    `when`(submissionService.claimNextQueuedSubmission()).thenReturn(submission)
    `when`(submissionRunner.runSubmission(submission)).thenReturn(result)
    scheduler.onApplicationReady()

    scheduler.processQueuedSubmissions()

    val order = inOrder(executor, submissionService, submissionRunner)
    order.verify(executor).cleanUpInterruptedExecutions()
    order.verify(submissionService).finishInterruptedSubmissions()
    order.verify(submissionService).claimNextQueuedSubmission()
    order.verify(submissionRunner).runSubmission(submission)
    order.verify(submissionService).finishSubmission(submission.id, result)
  }

  @Test
  fun `a failed final write triggers recovery on the next tick without rerunning the submission`() {
    val scheduler = createScheduler()
    val submission = mock(Submission::class.java)
    val result = SubmissionExecutionResult(SubmissionVerdict.ACCEPTED)
    `when`(submission.id).thenReturn(123L)
    `when`(executor.isAvailable("runtime")).thenReturn(true)
    `when`(submissionService.claimNextQueuedSubmission()).thenReturn(submission, null)
    `when`(submissionRunner.runSubmission(submission)).thenReturn(result)
    doThrow(IllegalStateException("connection lost")).`when`(submissionService).finishSubmission(123L, result)
    scheduler.onApplicationReady()

    scheduler.processQueuedSubmissions()
    scheduler.processQueuedSubmissions()

    verify(executor).cleanUpInterruptedExecutions()
    verify(submissionService, times(2)).finishInterruptedSubmissions()
    verify(submissionRunner).runSubmission(submission)
    verify(submissionService).finishSubmission(submission.id, result)
  }

  private fun createScheduler(properties: ExecutionProperties = this.properties) = SubmissionScheduler(
    submissionService,
    submissionRunner,
    executor,
    properties,
  )
}

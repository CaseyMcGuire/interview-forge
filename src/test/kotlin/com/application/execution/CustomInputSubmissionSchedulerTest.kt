package com.application.execution

import com.application.ent.CustomInputSubmission
import com.application.services.CodeExecutionSettingsService
import com.application.services.CustomInputSubmissionService
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class CustomInputSubmissionSchedulerTest {
  private val service = mock(CustomInputSubmissionService::class.java)
  private val settingsService = mock(CodeExecutionSettingsService::class.java)
  private val executor = mock(CodeExecutionService::class.java)
  private val startup = mock(ExecutionStartup::class.java)
  private val scheduler = CustomInputSubmissionScheduler(service, settingsService, executor, startup)

  @Test
  fun `preparation waits for shared startup recovery`() {
    `when`(startup.workersReady()).thenReturn(false)

    scheduler.prepareQueuedCustomInputSubmissions()

    verifyNoInteractions(service, settingsService, executor)
  }

  @Test
  fun `uncertain preparation completion recovers only the claimed run without executing reference code again`() {
    val run = mock(CustomInputSubmission::class.java)
    val program = PreparedProgram(emptyMap(), null, listOf("run"))
    val inputs = listOf(JsonNull)
    val output = CodeExecutionResult.Completed(
      ProgramStatus.SUCCEEDED,
      listOf(TestCaseExecutionResult(JsonNull, ProgramStatus.SUCCEEDED, JsonNull)),
    )
    `when`(run.id).thenReturn(123L)
    `when`(run.problemLanguageId).thenReturn(12L)
    `when`(startup.workersReady()).thenReturn(true)
    `when`(service.claimNextQueuedCustomInputSubmission()).thenReturn(run, null)
    `when`(service.loadCustomInputs(123L)).thenReturn(inputs)
    `when`(settingsService.loadReferenceSolutionSettings(12L)).thenReturn(CodeExecutionSettings("runtime", program, 1_000, 128))
    `when`(executor.executeCode("custom-reference-123", "runtime", program, inputs, 1_000, 128)).thenReturn(output)
    doThrow(IllegalStateException("connection lost"))
      .`when`(service).saveExpectedOutputsAndEnqueueGradingJob(123L, inputs)

    scheduler.prepareQueuedCustomInputSubmissions()
    scheduler.prepareQueuedCustomInputSubmissions()

    val order = inOrder(service, executor)
    order.verify(service).claimNextQueuedCustomInputSubmission()
    order.verify(service).loadCustomInputs(123L)
    order.verify(executor).executeCode("custom-reference-123", "runtime", program, inputs, 1_000, 128)
    order.verify(service).saveExpectedOutputsAndEnqueueGradingJob(123L, inputs)
    order.verify(service).finishInterruptedCustomPreparation(123L)
    order.verify(service).claimNextQueuedCustomInputSubmission()
    verify(executor).executeCode("custom-reference-123", "runtime", program, inputs, 1_000, 128)
    verify(service, never()).finishInterruptedCustomPreparations()
  }
}

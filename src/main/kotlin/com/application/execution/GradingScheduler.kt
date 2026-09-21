package com.application.execution

import com.application.ent.GradingJob
import com.application.services.CodeExecutionSettingsService
import com.application.services.GradingJobService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/** Grades ready jobs for problem submissions and custom input submissions. */
@Component
class GradingScheduler(
  private val gradingJobService: GradingJobService,
  private val settingsService: CodeExecutionSettingsService,
  private val codeGrader: CodeGrader,
  startup: ExecutionStartup,
) : AbstractScheduler(Duration.ofSeconds(1), startup::workersReady) {
  private val logger = LoggerFactory.getLogger(javaClass)
  // Retain this claim if its final write fails, even when the commit outcome is unknown.
  private var unfinishedJobId: Long? = null

  override fun executeTask() {
    recoverPreviousJob()
    gradeNextJob()
  }

  private fun recoverPreviousJob() {
    val id = unfinishedJobId ?: return
    gradingJobService.failGradingJob(id)
    unfinishedJobId = null
  }

  private fun gradeNextJob() {
    val job = gradingJobService.claimNextQueuedGradingJob() ?: return
    unfinishedJobId = job.id

    val result = gradeClaimedJob(job)

    // A failed final write may have committed. On the next tick, recover only this job without rerunning it.
    gradingJobService.finishGradingJob(job.id, result)
    unfinishedJobId = null
  }

  private fun gradeClaimedJob(job: GradingJob): GradingResult {
    try {
      val settings = settingsService.loadSubmittedCodeSettings(job.problemLanguageId, job.sourceCode)

      return codeGrader.gradeCode(
        executionId = "grading-job-${job.id}",
        runtime = settings.runtime,
        program = settings.program,
        cases = job.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
        timeLimitMs = settings.timeLimitMs,
        memoryLimitMb = settings.memoryLimitMb,
      )
    } catch (interruption: InterruptedException) {
      finishInterruptedGradingJob(job.id)
      throw interruption
    } catch (exception: Exception) {
      // Exception messages and raw diagnostics can contain private driver code or hidden inputs.
      logger.error("Grading failed for job {} ({})", job.id, exception.javaClass.simpleName)
      return GradingResult(
        outcome = GradingOutcome.INTERNAL_ERROR,
        caseResults = job.cases.map { TestCaseGradingResult(TestCaseOutcome.NOT_RUN) },
      )
    }
  }

  private fun finishInterruptedGradingJob(jobId: Long) {
    try {
      gradingJobService.failGradingJob(jobId)
      unfinishedJobId = null
    } finally {
      Thread.currentThread().interrupt()
    }
  }
}

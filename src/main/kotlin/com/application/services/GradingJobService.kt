package com.application.services

import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.GradingJob
import com.application.execution.CustomTestCaseResult
import com.application.execution.CustomTestSuiteRunResult
import com.application.execution.GradingOutcome
import com.application.execution.GradingResult
import com.application.execution.TestCaseGradingResult
import com.application.execution.TestCaseOutcome
import com.application.schema.CustomTestSuiteRunOutcome
import com.application.schema.CustomTestSuiteRunStatus
import com.application.schema.GradingJobStatus
import com.application.schema.SubmissionStatus
import com.application.security.ExecutionAccess
import org.springframework.stereotype.Service
import java.time.Instant

/** Claims persisted grading work and commits each result together with removal of its job. */
@Service
class GradingJobService(
  private val entClient: EntClient,
  private val submissionService: SubmissionService,
  private val customRunService: CustomTestSuiteRunService,
) {
  fun claimNextQueuedGradingJob(): GradingJob? = entClient.withTransaction { tx ->
    val job = tx.gradingJobs.indexes.status(GradingJobStatus.QUEUED).query {
      orderBy(GradingJob.createdAt.asc())
      orderBy(GradingJob.id.asc())
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return@withTransaction null

    val startedAt = Instant.now()
    startJobAttempt(tx, job, startedAt)

    tx.gradingJobs.update(job.id) {
      status = GradingJobStatus.RUNNING
      this.startedAt = startedAt
    }.saveAndLoad(ExecutionAccess.context).getOrThrow()
  }.getOrThrow()

  fun finishGradingJob(jobId: Long, result: GradingResult) {
    entClient.withTransaction { tx ->
      val job = lockRunningGradingJob(tx, jobId) ?: error("Grading job is missing")
      saveResultAndDeleteJob(tx, job, result)
    }.getOrThrow()
  }

  fun failGradingJob(jobId: Long) {
    entClient.withTransaction { tx ->
      // An uncertain completion may already have saved the result and removed the job.
      val job = lockRunningGradingJob(tx, jobId) ?: return@withTransaction
      val result = GradingResult(
        outcome = GradingOutcome.INTERNAL_ERROR,
        caseResults = job.cases.map { TestCaseGradingResult(TestCaseOutcome.NOT_RUN) },
      )

      saveResultAndDeleteJob(tx, job, result)
    }.getOrThrow()
  }

  /** Called during startup, after Docker cleanup and before either execution scheduler claims work. */
  fun finishInterruptedGradingJobs() {
    val interrupted = entClient.gradingJobs.indexes.status(GradingJobStatus.RUNNING).query()
      .all(ExecutionAccess.context).getOrThrow()

    for (job in interrupted) {
      failGradingJob(job.id)
    }
  }

  private fun lockRunningGradingJob(tx: EntTransactionClient, jobId: Long): GradingJob? {
    // Completion needs a row lock, which findById does not provide.
    val job = tx.gradingJobs.query { where(GradingJob.id eq jobId) }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return null

    check(job.status == GradingJobStatus.RUNNING) { "Grading job is not running" }
    return job
  }

  private fun startJobAttempt(tx: EntTransactionClient, job: GradingJob, startedAt: Instant) {
    val submissionId = job.submissionId
    val customRunId = job.customTestSuiteRunId

    when {
      submissionId != null -> startQueuedSubmission(tx, submissionId, startedAt)
      customRunId != null -> checkCustomRunIsRunning(tx, customRunId)
      else -> error("Grading job has no result destination")
    }
  }

  private fun startQueuedSubmission(tx: EntTransactionClient, submissionId: Long, startedAt: Instant) {
    val submission = tx.submissions.findById(ExecutionAccess.context, submissionId)
      .getOrThrow() ?: error("Submission is missing")

    check(submission.status == SubmissionStatus.QUEUED) { "Submission is not queued" }

    tx.submissions.update(submissionId) {
      status = SubmissionStatus.RUNNING
      this.startedAt = startedAt
    }.save(ExecutionAccess.context).getOrThrow()
  }

  private fun checkCustomRunIsRunning(tx: EntTransactionClient, customRunId: Long) {
    val run = tx.customTestSuiteRuns.findById(ExecutionAccess.context, customRunId)
      .getOrThrow() ?: error("Custom test suite run is missing")

    // Preparation already started this attempt; preserve its original start time.
    check(run.status == CustomTestSuiteRunStatus.RUNNING) { "Custom test suite run is not running" }
  }

  private fun saveResultAndDeleteJob(tx: EntTransactionClient, job: GradingJob, result: GradingResult) {
    val submissionId = job.submissionId
    val customRunId = job.customTestSuiteRunId

    check(result.caseResults.size == job.cases.size) { "Grading results must match the selected cases" }

    when {
      submissionId != null -> submissionService.finishSubmission(tx, submissionId, job.cases, result)
      customRunId != null -> customRunService.finishCustomTestSuiteRun(tx, customRunId, mapCustomRunResult(job, result))
      else -> error("Grading job has no result destination")
    }

    tx.gradingJobs.deleteById(ExecutionAccess.context, job.id).getOrThrow()
  }

  private fun mapCustomRunResult(job: GradingJob, result: GradingResult) = CustomTestSuiteRunResult(
    outcome = result.outcome.toCustomRunOutcome(),
    caseResults = result.caseResults.mapIndexed { index, caseResult ->
      CustomTestCaseResult(
        testCaseId = job.cases[index].testCaseId,
        outcome = caseResult.outcome,
        output = caseResult.execution?.stdout.orEmpty(),
      )
    },
    runtimeMs = result.runtimeMs,
  )

  private fun GradingOutcome.toCustomRunOutcome(): CustomTestSuiteRunOutcome = when (this) {
    GradingOutcome.PASSED -> CustomTestSuiteRunOutcome.PASSED
    GradingOutcome.COMPILE_ERROR -> CustomTestSuiteRunOutcome.COMPILE_ERROR
    GradingOutcome.WRONG_ANSWER -> CustomTestSuiteRunOutcome.WRONG_ANSWER
    GradingOutcome.INVALID_OUTPUT -> CustomTestSuiteRunOutcome.INVALID_OUTPUT
    GradingOutcome.RUNTIME_ERROR -> CustomTestSuiteRunOutcome.RUNTIME_ERROR
    GradingOutcome.TIME_LIMIT_EXCEEDED -> CustomTestSuiteRunOutcome.TIME_LIMIT_EXCEEDED
    GradingOutcome.MEMORY_LIMIT_EXCEEDED -> CustomTestSuiteRunOutcome.MEMORY_LIMIT_EXCEEDED
    GradingOutcome.OUTPUT_LIMIT_EXCEEDED -> CustomTestSuiteRunOutcome.OUTPUT_LIMIT_EXCEEDED
    GradingOutcome.INTERNAL_ERROR -> CustomTestSuiteRunOutcome.INTERNAL_ERROR
  }
}

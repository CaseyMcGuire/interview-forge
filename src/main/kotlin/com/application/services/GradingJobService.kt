package com.application.services

import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.GradingJob
import com.application.execution.CustomTestCaseResult
import com.application.execution.CustomInputSubmissionResult
import com.application.execution.GradingOutcome
import com.application.execution.GradingResult
import com.application.execution.TestCaseGradingResult
import com.application.execution.TestCaseOutcome
import com.application.schema.CustomInputSubmissionOutcome
import com.application.schema.CustomInputSubmissionStatus
import com.application.schema.GradingJobStatus
import com.application.schema.ProblemSubmissionStatus
import com.application.security.ExecutionAccess
import org.springframework.stereotype.Service
import java.time.Instant

/** Claims persisted grading work and commits each result together with removal of its job. */
@Service
class GradingJobService(
  private val entClient: EntClient,
  private val problemSubmissionService: ProblemSubmissionService,
  private val customInputSubmissionService: CustomInputSubmissionService,
) {
  fun claimNextQueuedGradingJob(): GradingJob? = entClient.withTransaction { tx ->
    val job = tx.gradingJobs.indexes.status(GradingJobStatus.QUEUED).query {
      orderBy(GradingJob.createdAt.asc())
      orderBy(GradingJob.id.asc())
    }
      .forUpdate()
      .skipLocked()
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
    val problemSubmissionId = job.problemSubmissionId
    val customInputSubmissionId = job.customInputSubmissionId

    when {
      problemSubmissionId != null -> startQueuedProblemSubmission(tx, problemSubmissionId, startedAt)
      customInputSubmissionId != null -> checkCustomInputSubmissionIsRunning(tx, customInputSubmissionId)
      else -> error("Grading job has no result destination")
    }
  }

  private fun startQueuedProblemSubmission(tx: EntTransactionClient, problemSubmissionId: Long, startedAt: Instant) {
    val problemSubmission = tx.problemSubmissions.findById(ExecutionAccess.context, problemSubmissionId)
      .getOrThrow() ?: error("Problem submission is missing")

    check(problemSubmission.status == ProblemSubmissionStatus.QUEUED) { "Problem submission is not queued" }

    tx.problemSubmissions.update(problemSubmissionId) {
      status = ProblemSubmissionStatus.RUNNING
      this.startedAt = startedAt
    }.save(ExecutionAccess.context).getOrThrow()
  }

  private fun checkCustomInputSubmissionIsRunning(tx: EntTransactionClient, customInputSubmissionId: Long) {
    val customInputSubmission = tx.customInputSubmissions.findById(ExecutionAccess.context, customInputSubmissionId)
      .getOrThrow() ?: error("Custom input submission is missing")

    // Preparation already started this attempt; preserve its original start time.
    check(customInputSubmission.status == CustomInputSubmissionStatus.RUNNING) {
      "Custom input submission is not running"
    }
  }

  private fun saveResultAndDeleteJob(tx: EntTransactionClient, job: GradingJob, result: GradingResult) {
    val problemSubmissionId = job.problemSubmissionId
    val customInputSubmissionId = job.customInputSubmissionId

    check(result.caseResults.size == job.cases.size) { "Grading results must match the selected cases" }

    when {
      problemSubmissionId != null -> problemSubmissionService.finishProblemSubmission(
        tx, problemSubmissionId, job.cases, result,
      )

      customInputSubmissionId != null -> customInputSubmissionService.finishCustomInputSubmission(
        tx, customInputSubmissionId, mapCustomInputSubmissionResult(job, result),
      )

      else -> error("Grading job has no result destination")
    }

    tx.gradingJobs.deleteById(ExecutionAccess.context, job.id).getOrThrow()
  }

  private fun mapCustomInputSubmissionResult(job: GradingJob, result: GradingResult) = CustomInputSubmissionResult(
    outcome = result.outcome.toCustomInputSubmissionOutcome(),
    caseResults = result.caseResults.mapIndexed { index, caseResult ->
      CustomTestCaseResult(
        testCaseId = job.cases[index].testCaseId,
        outcome = caseResult.outcome,
        output = caseResult.execution?.stdout.orEmpty(),
      )
    },
    runtimeMs = result.runtimeMs,
  )

  private fun GradingOutcome.toCustomInputSubmissionOutcome(): CustomInputSubmissionOutcome = when (this) {
    GradingOutcome.PASSED -> CustomInputSubmissionOutcome.PASSED
    GradingOutcome.COMPILE_ERROR -> CustomInputSubmissionOutcome.COMPILE_ERROR
    GradingOutcome.WRONG_ANSWER -> CustomInputSubmissionOutcome.WRONG_ANSWER
    GradingOutcome.INVALID_OUTPUT -> CustomInputSubmissionOutcome.INVALID_OUTPUT
    GradingOutcome.RUNTIME_ERROR -> CustomInputSubmissionOutcome.RUNTIME_ERROR
    GradingOutcome.TIME_LIMIT_EXCEEDED -> CustomInputSubmissionOutcome.TIME_LIMIT_EXCEEDED
    GradingOutcome.MEMORY_LIMIT_EXCEEDED -> CustomInputSubmissionOutcome.MEMORY_LIMIT_EXCEEDED
    GradingOutcome.OUTPUT_LIMIT_EXCEEDED -> CustomInputSubmissionOutcome.OUTPUT_LIMIT_EXCEEDED
    GradingOutcome.INTERNAL_ERROR -> CustomInputSubmissionOutcome.INTERNAL_ERROR
  }
}

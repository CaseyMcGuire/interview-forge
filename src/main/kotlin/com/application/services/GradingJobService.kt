package com.application.services

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.GradingJob
import com.application.execution.GradingExecutionSettings
import com.application.execution.GradingOutcome
import com.application.execution.GradingResult
import com.application.execution.LanguageExecutionConfig
import com.application.execution.TestCaseGradingResult
import com.application.execution.TestCaseOutcome
import com.application.schema.GradingJobStatus
import com.application.schema.ProblemCheckerKind
import com.application.schema.SubmissionStatus
import com.application.security.ExecutionAccess
import entkt.query.isNull
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import org.springframework.stereotype.Service
import java.time.Instant

/** Claims persisted grading work and commits each result together with removal of its job. */
@Service
class GradingJobService(
  private val entClient: EntClient,
  private val submissionService: SubmissionService,
  private val properties: ExecutionProperties,
  languageExecutionConfigs: List<LanguageExecutionConfig>,
) {
  private val languageConfigurations = languageExecutionConfigs.associateBy { it.key }
  private val publicContext = ViewerContext(Viewer.Anonymous)

  fun claimNextQueuedGradingJob(): GradingJob? = entClient.withTransaction { tx ->
    val job = tx.gradingJobs.indexes.status(GradingJobStatus.QUEUED).query {
      // Custom jobs join this worker when reference preparation and result routing are implemented.
      where(GradingJob.customTestSuiteRunId.isNull())
      orderBy(GradingJob.createdAt.asc())
      orderBy(GradingJob.id.asc())
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return@withTransaction null

    val submissionId = checkNotNull(job.submissionId)
    val submission = tx.submissions.findById(ExecutionAccess.context, submissionId)
      .getOrThrow() ?: error("Submission is missing")
    check(submission.status == SubmissionStatus.QUEUED) { "Submission is not queued" }

    val startedAt = Instant.now()
    tx.submissions.update(submissionId) {
      status = SubmissionStatus.RUNNING
      this.startedAt = startedAt
    }.save(ExecutionAccess.context).getOrThrow()

    tx.gradingJobs.update(job.id) {
      status = GradingJobStatus.RUNNING
      this.startedAt = startedAt
    }.saveAndLoad(ExecutionAccess.context).getOrThrow()
  }.getOrThrow()

  /** Reads current judge/runtime settings; the source and selected cases come from the claimed job. */
  fun loadExecutionSettings(job: GradingJob): GradingExecutionSettings =
    entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      val configuration = tx.problemLanguages.findById(publicContext, job.problemLanguageId)
        .getOrThrow() ?: error("Problem language is unavailable")

      val language = tx.languages.findById(publicContext, configuration.languageId)
        .getOrThrow() ?: error("Language is unavailable")

      val problem = tx.problems.findById(publicContext, configuration.problemId)
        .getOrThrow() ?: error("Problem is unavailable")
      check(problem.checkerKind == ProblemCheckerKind.EXACT_JSON) { "Unsupported checker" }

      val judge = tx.judgeConfigurations.indexes.problemLanguageId(configuration.id)
        .find(ExecutionAccess.context)
        .getOrThrow() ?: error("Judge is unavailable")

      val languageConfiguration = languageConfigurations[language.key] ?: error("Unsupported language")
      val runtime = properties.runtimes[language.key]?.takeIf { it.isNotBlank() }
        ?: error("Runtime is unavailable")

      GradingExecutionSettings(
        runtime = runtime,
        program = languageConfiguration.prepare(job.sourceCode, judge.testDriverCode),
        timeLimitMs = judge.timeLimitMs,
        memoryLimitMb = judge.memoryLimitMb,
      )
    }.getOrThrow()

  fun finishGradingJob(jobId: Long, result: GradingResult) {
    entClient.withTransaction { tx ->
      val job = lockRunningGradingJob(tx, jobId)
      saveResultAndDeleteJob(tx, job, result)
    }.getOrThrow()
  }

  fun failGradingJob(jobId: Long) {
    entClient.withTransaction { tx ->
      val job = lockRunningGradingJob(tx, jobId)
      val result = GradingResult(
        outcome = GradingOutcome.INTERNAL_ERROR,
        caseResults = job.cases.map { TestCaseGradingResult(TestCaseOutcome.NOT_RUN) },
      )

      saveResultAndDeleteJob(tx, job, result)
    }.getOrThrow()
  }

  /** Called between jobs after Docker cleanup; this recovery assumes one worker for the database. */
  fun finishInterruptedGradingJobs() {
    val interrupted = entClient.gradingJobs.indexes.status(GradingJobStatus.RUNNING).query {
      where(GradingJob.customTestSuiteRunId.isNull())
    }.all(ExecutionAccess.context).getOrThrow()

    for (job in interrupted) {
      failGradingJob(job.id)
    }
  }

  private fun lockRunningGradingJob(tx: EntTransactionClient, jobId: Long): GradingJob {
    // Completion needs a row lock, which findById does not provide.
    val job = tx.gradingJobs.query { where(GradingJob.id eq jobId) }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: error("Grading job is missing")

    check(job.status == GradingJobStatus.RUNNING) { "Grading job is not running" }
    return job
  }

  private fun saveResultAndDeleteJob(tx: EntTransactionClient, job: GradingJob, result: GradingResult) {
    val submissionId = checkNotNull(job.submissionId) { "Custom grading is not implemented yet" }
    submissionService.finishSubmission(tx, submissionId, job.cases, result)

    tx.gradingJobs.deleteById(ExecutionAccess.context, job.id).getOrThrow()
  }
}

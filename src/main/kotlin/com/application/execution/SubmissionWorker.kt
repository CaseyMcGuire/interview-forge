package com.application.execution

import com.application.config.ExecutionProperties
import com.application.ent.EntClient
import com.application.ent.EntTransactionClient
import com.application.ent.Submission
import com.application.ent.TestCase
import com.application.schema.ProblemCheckerKind
import com.application.schema.SubmissionKind
import com.application.schema.SubmissionStatus
import com.application.schema.SubmissionTestOutcome
import com.application.schema.SubmissionTestSource
import com.application.schema.SubmissionVerdict
import com.application.security.ExecutionAccess
import entkt.runtime.driver.IsolationLevel
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import org.slf4j.LoggerFactory
import java.time.Instant

/** Runs one persisted submission at a time, with database transactions outside program execution. */
class SubmissionWorker(
  private val entClient: EntClient,
  private val properties: ExecutionProperties,
  private val executor: ProgramExecutor,
  languageConfigurations: List<LanguageExecutionConfig>,
) {
  private val languages = languageConfigurations.associateBy { it.key }
  private val logger = LoggerFactory.getLogger(javaClass)
  private var initialized = false

  /** Returns false when the queue is empty. Only one worker may manage this database and runtime. */
  @Synchronized
  fun executeNextSubmission(): Boolean {
    initializeExecutor()
    finishInterruptedSubmissions()

    val submission = claimNextSubmission() ?: return false
    val result = executeSubmission(submission)

    // Do not retry a final write: a lost connection can leave its commit outcome unknown.
    saveExecutionResult(submission.id, result)
    return true
  }

  private fun initializeExecutor() {
    if (initialized) {
      return
    }

    executor.cleanUpInterruptedExecutions()
    initialized = true
  }

  private fun claimNextSubmission(): Submission? = entClient.withTransaction { tx ->
    val submission = tx.submissions.indexes.status(SubmissionStatus.QUEUED).query {
      where(Submission.kind eq SubmissionKind.SUBMIT)
      orderBy(Submission.createdAt.asc())
      orderBy(Submission.id.asc())
    }
      .forUpdate()
      .firstOrNull(ExecutionAccess.context)
      .getOrThrow()
      ?: return@withTransaction null

    tx.submissions.update(submission.id) {
      status = SubmissionStatus.RUNNING
      startedAt = Instant.now()
    }.save(ExecutionAccess.context).getOrThrow()

    submission
  }.getOrThrow()

  private fun executeSubmission(submission: Submission): ExecutionResult {
    try {
      val settings = loadExecutionSettings(submission)

      return executor.prepareProgram(settings.runtime, settings.program).use { program ->
        val compilation = program.compileProgram()
        if (compilation.status != ProgramStatus.SUCCEEDED) {
          return@use ExecutionResult(SubmissionVerdict.COMPILE_ERROR)
        }

        val result = program.runTestSuite(
          cases = settings.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
          timeLimitMs = settings.timeLimitMs,
          memoryLimitMb = settings.memoryLimitMb,
        )

        mapSuiteResult(settings.cases, result)
      }
    } catch (interruption: InterruptedException) {
      try {
        saveExecutionResult(submission.id, ExecutionResult(SubmissionVerdict.INTERNAL_ERROR))
      } finally {
        Thread.currentThread().interrupt()
      }

      throw interruption
    } catch (exception: Exception) {
      // Exception messages and raw diagnostics can contain private driver code or hidden inputs.
      logger.error("Execution failed for submission {} ({})", submission.id, exception.javaClass.simpleName)
      return ExecutionResult(SubmissionVerdict.INTERNAL_ERROR)
    }
  }

  private fun loadExecutionSettings(submission: Submission): ExecutionSettings =
    entClient.withTransaction(IsolationLevel.RepeatableRead) { tx ->
      val publicContext = ViewerContext(Viewer.Anonymous)
      val configuration = tx.problemLanguages.findById(publicContext, submission.problemLanguageId)
        .getOrThrow() ?: error("Problem language is unavailable")

      val language = tx.languages.findById(publicContext, configuration.languageId)
        .getOrThrow() ?: error("Language is unavailable")

      val problem = tx.problems.findById(publicContext, submission.problemId)
        .getOrThrow() ?: error("Problem is unavailable")

      check(problem.checkerKind == ProblemCheckerKind.EXACT_JSON) { "Unsupported checker" }

      val judge = tx.judgeConfigurations.indexes.problemLanguageId(configuration.id)
        .find(ExecutionAccess.context)
        .getOrThrow() ?: error("Judge is unavailable")

      val languageConfiguration = languages[language.key] ?: error("Unsupported language")
      val runtime = properties.runtimes[language.key]?.takeIf { it.isNotBlank() }
        ?: error("Runtime is unavailable")

      val cases = tx.testCases.indexes.problemId(problem.id).query {
        orderBy(TestCase.position.asc())
      }
        .all(ExecutionAccess.context)
        .getOrThrow()

      check(cases.isNotEmpty()) { "Official suite is empty" }

      tx.submissions.update(submission.id) {
        totalCases = cases.size
      }.save(ExecutionAccess.context).getOrThrow()

      ExecutionSettings(
        runtime = runtime,
        program = languageConfiguration.prepare(submission.sourceCode, judge.testDriverCode),
        timeLimitMs = judge.timeLimitMs,
        memoryLimitMb = judge.memoryLimitMb,
        cases = cases,
      )
    }.getOrThrow()

  private fun mapSuiteResult(cases: List<TestCase>, result: TestSuiteResult): ExecutionResult {
    val verdict = when (result.status) {
      TestSuiteStatus.PASSED -> SubmissionVerdict.ACCEPTED
      TestSuiteStatus.WRONG_ANSWER -> SubmissionVerdict.WRONG_ANSWER
      TestSuiteStatus.TIME_LIMIT_EXCEEDED -> SubmissionVerdict.TIME_LIMIT_EXCEEDED
      TestSuiteStatus.MEMORY_LIMIT_EXCEEDED -> SubmissionVerdict.MEMORY_LIMIT_EXCEEDED
      TestSuiteStatus.INVALID_OUTPUT,
      TestSuiteStatus.RUNTIME_ERROR,
      TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED -> SubmissionVerdict.RUNTIME_ERROR
    }

    return ExecutionResult(
      verdict = verdict,
      passedCases = result.passedCases,
      runtimeMs = result.runtimeMs,
      failedCase = result.failedCaseIndex?.let { cases[it] },
      suiteResult = result,
    )
  }

  private fun saveExecutionResult(submissionId: Long, result: ExecutionResult) {
    entClient.withTransaction { tx ->
      val submission = tx.submissions.query { where(Submission.id eq submissionId) }
        .forUpdate()
        .firstOrNull(ExecutionAccess.context)
        .getOrThrow()
        ?: error("Submission is missing")

      check(submission.status == SubmissionStatus.RUNNING) { "Submission already finished" }

      val failedCase = result.failedCase
      val output = result.suiteResult
      if (failedCase != null && output != null) {
        saveFailedCase(tx, submissionId, failedCase, output, result.verdict)
      }

      tx.submissions.update(submissionId) {
        status = SubmissionStatus.FINISHED
        verdict = result.verdict
        passedCases = maxOf(submission.passedCases, result.passedCases)
        runtimeMs = result.runtimeMs ?: submission.runtimeMs
        publicErrorMessage = publicErrorMessage(result.verdict)
        finishedAt = Instant.now()
      }.save(ExecutionAccess.context).getOrThrow()
    }.getOrThrow()
  }

  private fun saveFailedCase(
    tx: EntTransactionClient,
    submissionId: Long,
    testCase: TestCase,
    result: TestSuiteResult,
    verdict: SubmissionVerdict,
  ) {
    tx.submissionTestResults.create {
      this.submissionId = submissionId
      // Retain the selected input and visibility even if the original test changed during execution.
      position = testCase.position
      source = SubmissionTestSource.valueOf(testCase.visibility.name)
      inputJson = testCase.inputJson
      expectedOutputJson = testCase.expectedOutputJson
      outcome = SubmissionTestOutcome.valueOf(verdict.name)
      // PostgreSQL text cannot contain NUL. Keep hidden diagnostics bounded and execution-only.
      stdout = result.stdout.replace('\u0000', '\uFFFD').take(20_000)
      stderr = result.stderr.replace('\u0000', '\uFFFD').take(20_000)
      // The executor measures the entire suite, so individual case timing remains unknown.
    }.save(ExecutionAccess.context).getOrThrow()
  }

  private fun finishInterruptedSubmissions() {
    // Between attempts, RUNNING means a restart or a failed final write in this single-worker app.
    val interrupted = entClient.submissions.indexes.status(SubmissionStatus.RUNNING).query {
      where(Submission.kind eq SubmissionKind.SUBMIT)
    }.all(ExecutionAccess.context).getOrThrow()

    for (submission in interrupted) {
      saveExecutionResult(submission.id, ExecutionResult(SubmissionVerdict.INTERNAL_ERROR))
    }
  }

  private fun publicErrorMessage(verdict: SubmissionVerdict): String? = when (verdict) {
    SubmissionVerdict.COMPILE_ERROR -> "Compilation failed. Check the solution syntax and required function signature."
    SubmissionVerdict.RUNTIME_ERROR -> "The solution failed to return one JSON value within the output limit."
    SubmissionVerdict.TIME_LIMIT_EXCEEDED -> "Execution exceeded the time limit."
    SubmissionVerdict.MEMORY_LIMIT_EXCEEDED -> "Execution exceeded the memory limit."
    SubmissionVerdict.INTERNAL_ERROR -> "Execution could not complete. Submit the solution again."
    else -> null
  }

  private class ExecutionSettings(
    val runtime: String,
    val program: PreparedProgram,
    val timeLimitMs: Int,
    val memoryLimitMb: Int,
    val cases: List<TestCase>,
  )

  private class ExecutionResult(
    val verdict: SubmissionVerdict,
    val passedCases: Int = 0,
    val runtimeMs: Long? = null,
    val failedCase: TestCase? = null,
    val suiteResult: TestSuiteResult? = null,
  )
}

package com.application.execution

import com.application.ent.Submission
import com.application.ent.TestCase
import com.application.schema.SubmissionVerdict
import com.application.services.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** Executes a claimed submission and returns its result after releasing runtime resources. */
@Component
class SubmissionRunner(
  private val submissionService: SubmissionService,
  private val executor: ProgramExecutor,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun runSubmission(submission: Submission): SubmissionExecutionResult {
    try {
      val settings = submissionService.loadExecutionSettings(submission)
      return compileAndRunProgram(submission.id, settings)
    } catch (interruption: InterruptedException) {
      throw interruption
    } catch (exception: Exception) {
      // Exception messages and raw diagnostics can contain private driver code or hidden inputs.
      logger.error("Execution failed for submission {} ({})", submission.id, exception.javaClass.simpleName)
      return SubmissionExecutionResult(SubmissionVerdict.INTERNAL_ERROR)
    }
  }

  private fun compileAndRunProgram(
    submissionId: Long,
    settings: SubmissionExecutionSettings,
  ): SubmissionExecutionResult {
    val program = executor.prepareProgram(submissionId, settings.runtime, settings.program)

    try {
      val compilation = program.compileProgram()
      if (compilation.status != ProgramStatus.SUCCEEDED) {
        return SubmissionExecutionResult(SubmissionVerdict.COMPILE_ERROR)
      }

      val result = program.runTestSuite(
        cases = settings.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
        timeLimitMs = settings.timeLimitMs,
        memoryLimitMb = settings.memoryLimitMb,
      )

      return mapSuiteResult(settings.cases, result)
    } finally {
      program.close()
    }
  }

  private fun mapSuiteResult(cases: List<TestCase>, result: TestSuiteResult): SubmissionExecutionResult {
    val verdict = when (result.status) {
      TestSuiteStatus.PASSED -> SubmissionVerdict.ACCEPTED
      TestSuiteStatus.WRONG_ANSWER -> SubmissionVerdict.WRONG_ANSWER
      TestSuiteStatus.TIME_LIMIT_EXCEEDED -> SubmissionVerdict.TIME_LIMIT_EXCEEDED
      TestSuiteStatus.MEMORY_LIMIT_EXCEEDED -> SubmissionVerdict.MEMORY_LIMIT_EXCEEDED
      TestSuiteStatus.INVALID_OUTPUT,
      TestSuiteStatus.RUNTIME_ERROR,
      TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED -> SubmissionVerdict.RUNTIME_ERROR
    }

    return SubmissionExecutionResult(
      verdict = verdict,
      passedCases = result.passedCases,
      runtimeMs = result.runtimeMs,
      failedCase = result.failedCaseIndex?.let { cases[it] },
      suiteResult = result,
    )
  }
}

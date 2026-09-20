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
  private val codeGrader: CodeGrader,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun runSubmission(submission: Submission): SubmissionExecutionResult {
    try {
      val settings = submissionService.loadExecutionSettings(submission)
      val result = codeGrader.gradeCode(
        executionId = "submission-${submission.id}",
        runtime = settings.runtime,
        program = settings.program,
        cases = settings.cases.map { TestCaseInput(it.inputJson, it.expectedOutputJson) },
        timeLimitMs = settings.timeLimitMs,
        memoryLimitMb = settings.memoryLimitMb,
      )

      return mapGradingResult(settings.cases, result)
    } catch (interruption: InterruptedException) {
      throw interruption
    } catch (exception: Exception) {
      // Exception messages and raw diagnostics can contain private driver code or hidden inputs.
      logger.error("Execution failed for submission {} ({})", submission.id, exception.javaClass.simpleName)
      return SubmissionExecutionResult(SubmissionVerdict.INTERNAL_ERROR)
    }
  }

  private fun mapGradingResult(cases: List<TestCase>, result: GradingResult): SubmissionExecutionResult {
    val failedCaseIndex = result.caseResults.indexOfFirst {
      it.outcome != TestCaseOutcome.PASSED && it.outcome != TestCaseOutcome.NOT_RUN
    }

    return SubmissionExecutionResult(
      verdict = result.outcome.toSubmissionVerdict(),
      passedCases = result.passedCases,
      runtimeMs = result.runtimeMs,
      failedCase = cases.getOrNull(failedCaseIndex),
      failedCaseResult = result.caseResults.getOrNull(failedCaseIndex),
    )
  }
}

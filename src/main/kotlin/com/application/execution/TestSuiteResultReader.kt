package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Incomplete reports never count as success, including a solution calling System.exit(0). */
internal fun readTestSuiteResult(execution: ProgramResult, caseCount: Int): TestSuiteResult {
  var startedCases = 0
  var reportedResult: TestSuiteResult? = null

  val validReport = runCatching {
    for (line in execution.stdout.lineSequence().filter { it.isNotBlank() }) {
      check(reportedResult == null) { "Output followed the suite result" }
      val event = Json.parseToJsonElement(line).jsonObject
      val caseIndex = event["caseIndex"]?.jsonPrimitive?.int

      if (caseIndex != null) {
        check(caseIndex == startedCases && caseIndex < caseCount) { "Unexpected case checkpoint" }
        startedCases++
      } else {
        reportedResult = TestSuiteProtocol.decodeResult(event)
        validateSuiteResult(checkNotNull(reportedResult), startedCases, caseCount)
      }
    }
  }.isSuccess

  val result = reportedResult?.takeIf { validReport && execution.status == ProgramStatus.SUCCEEDED }
  if (result != null) {
    return TestSuiteResult(
      result.status, result.passedCases, result.failedCaseIndex,
      result.stdout, result.stderr, execution.runtimeMs,
    )
  }

  val status = when (execution.status) {
    ProgramStatus.TIME_LIMIT_EXCEEDED -> TestSuiteStatus.TIME_LIMIT_EXCEEDED
    ProgramStatus.MEMORY_LIMIT_EXCEEDED -> TestSuiteStatus.MEMORY_LIMIT_EXCEEDED
    ProgramStatus.OUTPUT_LIMIT_EXCEEDED -> TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED
    else -> TestSuiteStatus.RUNTIME_ERROR
  }
  val failedCaseIndex = (startedCases - 1).takeIf { it >= 0 }

  return TestSuiteResult(
    status = status,
    passedCases = failedCaseIndex ?: 0,
    failedCaseIndex = failedCaseIndex,
    stderr = execution.stderr,
    runtimeMs = execution.runtimeMs,
  )
}

private fun validateSuiteResult(result: TestSuiteResult, startedCases: Int, caseCount: Int) {
  if (result.status == TestSuiteStatus.PASSED) {
    check(startedCases == caseCount && result.passedCases == caseCount && result.failedCaseIndex == null)
    check(result.stdout.isEmpty() && result.stderr.isEmpty())
  } else {
    check(startedCases > 0 && result.passedCases == startedCases - 1)
    check(result.failedCaseIndex == result.passedCases)
  }

  check(result.stdout.length <= TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES)
  check(result.stderr.length <= TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES)
}

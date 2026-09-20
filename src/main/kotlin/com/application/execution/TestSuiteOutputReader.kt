package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Preserves completed cases after a JVM failure; a missing or malformed report cannot establish success. */
internal fun readTestSuiteOutput(
  execution: ProgramResult,
  inputs: List<JsonElement>,
): CodeExecutionResult.Completed {
  val outputChecker = JsonOutputChecker()
  val caseResults = mutableListOf<TestCaseExecutionResult>()
  var startedCases = 0
  var reportedStatus: ProgramStatus? = null

  val validReport = runCatching {
    for (line in execution.stdout.lineSequence().filter { it.isNotBlank() }) {
      check(reportedStatus == null) { "Output followed the suite result" }
      val event = Json.parseToJsonElement(line).jsonObject

      when (event.getValue("event").jsonPrimitive.content) {
        "caseStarted" -> {
          val index = event.getValue("caseIndex").jsonPrimitive.int
          check(index == startedCases && index < inputs.size && startedCases == caseResults.size) {
            "Unexpected case checkpoint"
          }

          startedCases++
        }

        "caseFinished" -> {
          val index = event.getValue("caseIndex").jsonPrimitive.int
          check(index == caseResults.size && startedCases == index + 1) { "Unexpected case result" }

          caseResults += readCaseExecutionResult(event, inputs[index], outputChecker)
        }

        "suiteFinished" -> {
          val status = ProgramStatus.valueOf(event.getValue("status").jsonPrimitive.content)
          check(startedCases == caseResults.size) { "Missing case result" }
          if (status == ProgramStatus.SUCCEEDED) {
            check(caseResults.size == inputs.size) { "Incomplete suite results" }
          }

          reportedStatus = status
        }

        else -> error("Unknown suite event")
      }
    }
  }.isSuccess

  val status = when {
    execution.status != ProgramStatus.SUCCEEDED -> execution.status
    !validReport -> ProgramStatus.FAILED
    else -> reportedStatus ?: ProgramStatus.FAILED
  }

  if (startedCases > caseResults.size) {
    // A checkpoint identifies a case that died before reporting; its elapsed time is unknown.
    caseResults += TestCaseExecutionResult(
      inputJson = inputs[caseResults.size],
      status = status,
      stderr = boundedCaseOutput(execution.stderr),
    )
  }

  return CodeExecutionResult.Completed(status, caseResults, execution.runtimeMs)
}

private fun readCaseExecutionResult(
  event: JsonObject,
  input: JsonElement,
  outputChecker: JsonOutputChecker,
): TestCaseExecutionResult {
  val output = TestSuiteProtocol.decodeCaseOutput(event)
  check(output.stdout.toByteArray(Charsets.UTF_8).size <= TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES)
  check(output.stderr.toByteArray(Charsets.UTF_8).size <= TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES)
  val runtimeMs = output.runtimeMs
  check(runtimeMs == null || runtimeMs >= 0) { "Invalid case duration" }

  return TestCaseExecutionResult(
    inputJson = input,
    status = output.status,
    outputJson = if (output.status == ProgramStatus.SUCCEEDED) outputChecker.parseOutput(output.stdout) else null,
    stdout = output.stdout,
    stderr = output.stderr,
    runtimeMs = output.runtimeMs,
  )
}

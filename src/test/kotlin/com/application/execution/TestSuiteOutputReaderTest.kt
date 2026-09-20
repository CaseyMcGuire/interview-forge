package com.application.execution

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestSuiteOutputReaderTest {
  private val inputs = listOf(1, 2, 3).map(::JsonPrimitive)

  @Test
  fun `partial inconsistent duplicated and malformed reports never pass`() {
    val started = TestSuiteProtocol.encodeCaseStarted(0)
    val finished = TestSuiteProtocol.encodeCaseFinished(0, ProgramResult(ProgramStatus.SUCCEEDED, "1"))
    val completed = TestSuiteProtocol.encodeSuiteFinished(ProgramStatus.SUCCEEDED)

    for (output in listOf(
      "",
      "$started\n",
      "$started\n$completed\n",
      "$started\n$finished\n$completed\n",
      "$started\n$finished\n$finished\n",
      "$finished\n",
      "${TestSuiteProtocol.encodeCaseStarted(1)}\n",
      "$started\n{\"event\":\"unknown\"}\n",
    )) {
      val result = readTestSuiteOutput(ProgramResult(ProgramStatus.SUCCEEDED, output), inputs)
      assertEquals(ProgramStatus.FAILED, result.status, output)
    }

    val trailingOutput = "$started\n$finished\n$completed\nnot JSON\n"
    assertEquals(
      ProgramStatus.FAILED,
      readTestSuiteOutput(ProgramResult(ProgramStatus.SUCCEEDED, trailingOutput), inputs.take(1)).status,
    )
  }

  @Test
  fun `a hard JVM failure preserves completed results and identifies the unfinished invocation`() {
    val output = listOf(
      TestSuiteProtocol.encodeCaseStarted(0),
      TestSuiteProtocol.encodeCaseFinished(0, ProgramResult(ProgramStatus.SUCCEEDED, "1", runtimeMs = 7)),
      TestSuiteProtocol.encodeCaseStarted(1),
      "Terminating due to java.lang.OutOfMemoryError",
    ).joinToString("\n")
    val execution = ProgramResult(ProgramStatus.MEMORY_LIMIT_EXCEEDED, output, "fatal diagnostic", runtimeMs = 50)

    val result = readTestSuiteOutput(execution, inputs)

    assertEquals(ProgramStatus.MEMORY_LIMIT_EXCEEDED, result.status)
    assertEquals(50L, result.runtimeMs)
    assertEquals(2, result.caseResults.size)
    assertEquals(JsonPrimitive(1), result.caseResults[0].outputJson)
    assertEquals(7L, result.caseResults[0].runtimeMs)
    assertEquals(inputs[1], result.caseResults[1].inputJson)
    assertEquals(ProgramStatus.MEMORY_LIMIT_EXCEEDED, result.caseResults[1].status)
    assertEquals("fatal diagnostic", result.caseResults[1].stderr)
    assertNull(result.caseResults[1].runtimeMs)
    assertNull(result.caseResults[1].outputJson)
  }

  @Test
  fun `successful suite completion can include failed invocations and later JSON null answers`() {
    val outputs = listOf(
      ProgramResult(ProgramStatus.FAILED, "1", "exception", runtimeMs = 1),
      ProgramResult(ProgramStatus.SUCCEEDED, "not JSON", runtimeMs = 2),
      ProgramResult(ProgramStatus.SUCCEEDED, "null", runtimeMs = 3),
    )
    val report = outputs.flatMapIndexed { index, output ->
      listOf(TestSuiteProtocol.encodeCaseStarted(index), TestSuiteProtocol.encodeCaseFinished(index, output))
    } + TestSuiteProtocol.encodeSuiteFinished(ProgramStatus.SUCCEEDED)

    val result = readTestSuiteOutput(ProgramResult(ProgramStatus.SUCCEEDED, report.joinToString("\n")), inputs)

    assertEquals(ProgramStatus.SUCCEEDED, result.status)
    assertEquals(inputs, result.caseResults.map { it.inputJson })
    assertEquals(outputs.map { it.status }, result.caseResults.map { it.status })
    assertEquals(outputs.map { it.runtimeMs }, result.caseResults.map { it.runtimeMs })
    assertEquals(outputs.map { it.stdout }, result.caseResults.map { it.stdout })
    assertEquals(outputs.map { it.stderr }, result.caseResults.map { it.stderr })
    assertEquals(listOf(null, null, JsonNull), result.caseResults.map { it.outputJson })
  }

  @Test
  fun `invalid case durations and oversized output are rejected`() {
    for (output in listOf(
      ProgramResult(ProgramStatus.SUCCEEDED, "1", runtimeMs = -1),
      ProgramResult(ProgramStatus.SUCCEEDED, "é".repeat(10_001)),
      ProgramResult(ProgramStatus.SUCCEEDED, "1", stderr = "x".repeat(20_001)),
    )) {
      val report = listOf(
        TestSuiteProtocol.encodeCaseStarted(0),
        TestSuiteProtocol.encodeCaseFinished(0, output),
        TestSuiteProtocol.encodeSuiteFinished(ProgramStatus.SUCCEEDED),
      ).joinToString("\n")

      val result = readTestSuiteOutput(ProgramResult(ProgramStatus.SUCCEEDED, report), inputs.take(1))

      assertEquals(ProgramStatus.FAILED, result.status)
      assertEquals(ProgramStatus.FAILED, result.caseResults.single().status)
      assertNull(result.caseResults.single().outputJson)
    }
  }
}

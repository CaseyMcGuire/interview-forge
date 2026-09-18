package com.application.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestSuiteResultReaderTest {
  @Test
  fun `partial or inconsistent success reports never pass`() {
    for (output in listOf(
      "",
      "{\"caseIndex\":0}\n",
      "{\"caseIndex\":0}\n{\"status\":\"PASSED\",\"passedCases\":2,\"stdout\":\"\",\"stderr\":\"\"}\n",
      "{\"caseIndex\":0}\n{\"status\":\"PASSED\",\"passedCases\":1,\"stdout\":\"\",\"stderr\":\"\"}\n",
    )) {
      val result = readTestSuiteResult(ProgramResult(ProgramStatus.SUCCEEDED, output), 2)
      assertEquals(TestSuiteStatus.RUNTIME_ERROR, result.status)
    }
  }

  @Test
  fun `a hard JVM failure retains the last case checkpoint despite non-JSON fatal diagnostics`() {
    val output = "{\"caseIndex\":0}\n{\"caseIndex\":1}\nTerminating due to java.lang.OutOfMemoryError\n"
    val result = readTestSuiteResult(ProgramResult(ProgramStatus.MEMORY_LIMIT_EXCEEDED, output), 3)

    assertEquals(TestSuiteStatus.MEMORY_LIMIT_EXCEEDED, result.status)
    assertEquals(1, result.passedCases)
    assertEquals(1, result.failedCaseIndex)
    assertEquals("", result.stdout)
  }
}

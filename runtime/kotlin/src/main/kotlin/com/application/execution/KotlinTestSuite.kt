package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Timer
import java.util.TimerTask

/** Invokes the same test driver in one JVM, preserving solution state across the ordered suite. */
object KotlinTestSuite {
  @JvmStatic
  fun main(arguments: Array<String>) {
    val input = Json.parseToJsonElement(System.`in`.bufferedReader().readText()).jsonObject
    val cases = TestSuiteProtocol.decodeCases(input)
    val timeLimitMs = input.getValue("timeLimitMs").jsonPrimitive.int
    require(cases.isNotEmpty() && timeLimitMs in 1..60_000)

    val report = System.out
    val checker = JsonOutputChecker()
    val timer = Timer("test-case-deadline", true)
    val driver by lazy { loadTestDriver(arguments.single()) }

    try {
      for ((index, case) in cases.withIndex()) {
        // A checkpoint identifies the failing case even if the JVM exits or runs out of memory.
        report.println(TestSuiteProtocol.encodeCaseStarted(index))
        report.flush()

        val failure = runTestCase(index, case, timeLimitMs, timer, report, checker) { driver }
        if (failure != null) {
          reportResult(report, failure)
          Runtime.getRuntime().halt(0)
        }
      }

      reportResult(report, TestSuiteResult(TestSuiteStatus.PASSED, cases.size))
    } finally {
      timer.cancel()
    }
  }

  /**
   * Rebinds only the driver's streams; its classes and solution state stay loaded between cases.
   * Completion and the watchdog share a lock so a late deadline cannot kill the next case.
   */
  private fun runTestCase(
    index: Int,
    case: TestCaseInput,
    timeLimitMs: Int,
    timer: Timer,
    report: PrintStream,
    checker: JsonOutputChecker,
    driver: () -> Method,
  ): TestSuiteResult? {
    val stdout = CaseOutput()
    val stderr = CaseOutput()
    val originalInput = System.`in`
    val originalOutput = System.out
    val originalError = System.err
    val deadlineLock = Any()
    var running = true

    val deadline = object : TimerTask() {
      override fun run() {
        synchronized(deadlineLock) {
          if (running) {
            val status = if (stdout.limitExceeded || stderr.limitExceeded) {
              TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED
            } else {
              TestSuiteStatus.TIME_LIMIT_EXCEEDED
            }

            reportResult(report, TestSuiteResult(status, index, index, stdout.text(), stderr.text()))
            // A looping solution cannot be stopped safely by interrupting a Java thread.
            Runtime.getRuntime().halt(0)
          }
        }
      }
    }

    var failure: Throwable? = null
    try {
      System.setIn(case.input.toString().byteInputStream(Charsets.UTF_8))
      System.setOut(PrintStream(stdout, true, Charsets.UTF_8))
      System.setErr(PrintStream(stderr, true, Charsets.UTF_8))
      timer.schedule(deadline, timeLimitMs.toLong())

      invokeTestDriver(driver())
    } catch (exception: Throwable) {
      failure = if (exception is InvocationTargetException) exception.targetException else exception
    } finally {
      synchronized(deadlineLock) { running = false }
      deadline.cancel()
      System.setIn(originalInput)
      System.setOut(originalOutput)
      System.setErr(originalError)
    }

    val status = when {
      stdout.limitExceeded || stderr.limitExceeded -> TestSuiteStatus.OUTPUT_LIMIT_EXCEEDED
      failure is OutOfMemoryError -> TestSuiteStatus.MEMORY_LIMIT_EXCEEDED
      failure != null -> TestSuiteStatus.RUNTIME_ERROR
      else -> when (checker.checkOutput(case.expectedOutput, stdout.text())) {
        JsonOutputCheckResult.MATCH -> return null
        JsonOutputCheckResult.MISMATCH -> TestSuiteStatus.WRONG_ANSWER
        JsonOutputCheckResult.INVALID_OUTPUT -> TestSuiteStatus.INVALID_OUTPUT
      }
    }

    val diagnostic = if (failure != null) {
      (stderr.text() + "\n" + failure.javaClass.simpleName).take(TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES)
    } else {
      stderr.text()
    }
    return TestSuiteResult(status, index, index, stdout.text(), diagnostic)
  }

  private fun loadTestDriver(className: String): Method {
    val driver = Class.forName(className)
    return try {
      driver.getMethod("main")
    } catch (_: NoSuchMethodException) {
      driver.getMethod("main", Array<String>::class.java)
    }
  }

  private fun invokeTestDriver(driver: Method) {
    if (driver.parameterCount == 0) {
      driver.invoke(null)
    } else {
      driver.invoke(null, emptyArray<String>() as Any)
    }
  }

  private fun reportResult(output: PrintStream, result: TestSuiteResult) {
    output.println(TestSuiteProtocol.encodeResult(result))
    output.flush()
  }
}

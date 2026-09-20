package com.application.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

/** Invokes the same test driver in one JVM, preserving solution state across the ordered suite. */
object KotlinTestSuite {
  @JvmStatic
  fun main(arguments: Array<String>) {
    val input = Json.parseToJsonElement(System.`in`.bufferedReader().readText()).jsonObject
    val inputs = TestSuiteProtocol.decodeInputs(input)
    val timeLimitMs = input.getValue("timeLimitMs").jsonPrimitive.int
    require(inputs.isNotEmpty() && timeLimitMs in 1..60_000)

    val report = System.out
    val timer = Timer("test-case-deadline", true)
    val driver by lazy { loadTestDriver(arguments.single()) }

    try {
      for ((index, caseInput) in inputs.withIndex()) {
        // A checkpoint identifies the failing case even if the JVM exits or runs out of memory.
        report.println(TestSuiteProtocol.encodeCaseStarted(index))
        report.flush()

        val result = runTestCase(index, caseInput, timeLimitMs, timer, report) { driver }
        report.println(TestSuiteProtocol.encodeCaseFinished(index, result))
        report.flush()

        if (result.status == ProgramStatus.MEMORY_LIMIT_EXCEEDED) {
          reportSuiteFinished(report, result.status)
          Runtime.getRuntime().halt(0)
        }
      }

      reportSuiteFinished(report, ProgramStatus.SUCCEEDED)
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
    input: JsonElement,
    timeLimitMs: Int,
    timer: Timer,
    report: PrintStream,
    driver: () -> Method,
  ): ProgramResult {
    val stdout = CaseOutput()
    val stderr = CaseOutput()
    val originalInput = System.`in`
    val originalOutput = System.out
    val originalError = System.err
    val deadlineLock = Any()
    val startedAt = System.nanoTime()
    var running = true

    val deadline = object : TimerTask() {
      override fun run() {
        synchronized(deadlineLock) {
          if (running) {
            val status = if (stdout.limitExceeded || stderr.limitExceeded) {
              ProgramStatus.OUTPUT_LIMIT_EXCEEDED
            } else {
              ProgramStatus.TIME_LIMIT_EXCEEDED
            }

            val runtimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            val output = ProgramResult(status, stdout.text(), stderr.text(), runtimeMs)
            report.println(TestSuiteProtocol.encodeCaseFinished(index, output))
            reportSuiteFinished(report, status)
            // A looping solution cannot be stopped safely by interrupting a Java thread.
            Runtime.getRuntime().halt(0)
          }
        }
      }
    }

    var failure: Throwable? = null
    try {
      System.setIn(input.toString().byteInputStream(Charsets.UTF_8))
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
      stdout.limitExceeded || stderr.limitExceeded -> ProgramStatus.OUTPUT_LIMIT_EXCEEDED
      failure is OutOfMemoryError -> ProgramStatus.MEMORY_LIMIT_EXCEEDED
      failure != null -> ProgramStatus.FAILED
      else -> ProgramStatus.SUCCEEDED
    }

    val diagnostic = if (failure != null) {
      boundedCaseOutput(stderr.text() + "\n" + failure.javaClass.simpleName)
    } else {
      stderr.text()
    }
    val runtimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
    return ProgramResult(status, stdout.text(), diagnostic, runtimeMs)
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

  private fun reportSuiteFinished(output: PrintStream, status: ProgramStatus) {
    output.println(TestSuiteProtocol.encodeSuiteFinished(status))
    output.flush()
  }
}

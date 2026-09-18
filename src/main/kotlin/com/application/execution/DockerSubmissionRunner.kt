package com.application.execution

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.HealthCheck
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.Ulimit
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Compiles one submission and runs its test cases, then releases its disposable workspace. */
internal class DockerSubmissionRunner(
  private val docker: DockerClient,
  private val runtime: String,
  private val program: PreparedProgram,
  private val workspace: Path,
  private val containerLabels: Map<String, String>,
) : ProgramExecution {
  override fun compileProgram(): ProgramResult {
    val command = program.compileCommand ?: return ProgramResult(ProgramStatus.SUCCEEDED)

    val result = runContainer(command, "", 60_000, 1_024, writable = true)

    if (result.status == ProgramStatus.SUCCEEDED) {
      program.sourceFiles.keys.forEach { Files.deleteIfExists(workspace.resolve(it)) }
    }

    return result
  }

  override fun runTestSuite(cases: List<TestCaseInput>, timeLimitMs: Int, memoryLimitMb: Int): TestSuiteResult {
    require(cases.isNotEmpty())
    require(timeLimitMs in 1..60_000)

    // Per-case deadlines run inside the JVM. This outer budget also bounds startup and leaked threads.
    val suiteLimitMs = (cases.size.toLong() * timeLimitMs + 2_000).coerceAtMost(600_000).toInt()
    val input = TestSuiteProtocol.encodeInput(cases, timeLimitMs)
    // The terminal report can escape two 20 KB streams, plus a short checkpoint for each case.
    val reportLimit = Math.addExact(TestSuiteProtocol.MAX_RESULT_BYTES, Math.multiplyExact(cases.size, 64))
    val result = runContainer(
      program.runCommand, input, suiteLimitMs, memoryLimitMb,
      writable = false,
      output = ContainerOutput(reportLimit),
    )

    return readTestSuiteResult(result, cases.size)
  }

  override fun close() {
    deleteExecutionWorkspace(workspace)
  }

  private fun runContainer(
    command: List<String>,
    input: String,
    timeLimitMs: Int,
    memoryLimitMb: Int,
    writable: Boolean,
    output: ContainerOutput = ContainerOutput(),
  ): ProgramResult {
    val name = "interview-forge-${UUID.randomUUID()}"
    val inputFile = Files.createTempFile(workspace.parent, "submission-input-", ".txt")

    try {
      Files.writeString(inputFile, input)
      Files.setPosixFilePermissions(inputFile, PosixFilePermissions.fromString("r--r--r--"))

      createExecutionContainer(name, inputFile, command, timeLimitMs, memoryLimitMb, writable)
      attachOutputAndStartContainer(name, output)

      return awaitExecutionResult(name, output, timeLimitMs)
    } catch (exception: RuntimeException) {
      // Docker's HTTP client wraps interrupted socket operations. Clear the flag for cleanup;
      // propagate cancellation consistently so the worker can finish the attempt and restore it.
      if (!Thread.interrupted()) {
        throw exception
      }

      throw InterruptedException("Docker execution was interrupted").apply { initCause(exception) }
    } finally {
      cleanUpContainer(name, output, inputFile)
    }
  }

  private fun attachOutputAndStartContainer(name: String, output: ContainerOutput) {
    // Attach before starting so even a short-lived program's output is captured.
    docker.attachContainerCmd(name)
      .withStdOut(true)
      .withStdErr(true)
      .withFollowStream(true)
      .exec(output)

    check(output.awaitStarted(10, TimeUnit.SECONDS)) { "Could not attach container output" }
    docker.startContainerCmd(name).exec()
  }

  private fun cleanUpContainer(name: String, output: ContainerOutput, inputFile: Path) {
    try {
      docker.removeExecutionContainer(name)
    } finally {
      try {
        output.close()
      } finally {
        Files.deleteIfExists(inputFile)
      }
    }
  }

  private fun awaitExecutionResult(name: String, output: ContainerOutput, timeLimitMs: Int): ProgramResult {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeLimitMs.toLong() + 2_000)
    val outputClosed = output.awaitCompletion(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)

    if (output.limitExceeded) {
      return output.toProgramResult(ProgramStatus.OUTPUT_LIMIT_EXCEEDED)
    }

    if (!outputClosed || !waitForContainerExit(name, deadline)) {
      return output.toProgramResult(ProgramStatus.TIME_LIMIT_EXCEEDED)
    }

    return readExecutionResult(name, output, timeLimitMs)
  }

  private fun waitForContainerExit(name: String, deadline: Long): Boolean {
    // Closing stdout/stderr alone does not mean the program has exited.
    val completion = docker.waitContainerCmd(name).exec(WaitContainerResultCallback())
    return completion.awaitCompletion(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)
  }

  private fun createExecutionContainer(
    name: String,
    inputFile: Path,
    command: List<String>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
    writable: Boolean,
  ) {
    val host = createContainerHostConfig(inputFile, memoryLimitMb, writable)
    val duration = BigDecimal.valueOf(timeLimitMs.toLong(), 3).toPlainString() + "s"

    // A file supplies EOF reliably; docker-java's attached stdin does not half-close the socket.
    // Command arguments remain separate values and are never interpolated into shell code.
    val commandWithInput = listOf(
      "--kill-after=0.1s", duration,
      "/bin/sh", "-c", "exec \"${'$'}@\" < /run/submission-input", "submission",
    ) + command

    docker.createContainerCmd(runtime)
      .withName(name)
      .withLabels(containerLabels)
      .withHostConfig(host)
      .withUser("65534:65534")
      .withWorkingDir("/workspace")
      .withHealthcheck(HealthCheck().withTest(listOf("NONE")))
      // The container also enforces the deadline if the application crashes or disconnects.
      .withEntrypoint("/usr/bin/timeout")
      .withCmd(commandWithInput)
      .exec()
  }

  private fun createContainerHostConfig(
    inputFile: Path,
    memoryLimitMb: Int,
    writable: Boolean,
  ): HostConfig {
    val mount = Mount()
      .withType(MountType.BIND)
      .withSource(workspace.toString())
      .withTarget("/workspace")
      .withReadOnly(!writable)
    val inputMount = Mount()
      .withType(MountType.BIND)
      .withSource(inputFile.toString())
      .withTarget("/run/submission-input")
      .withReadOnly(true)
    val memoryBytes = memoryLimitMb.toLong() * 1_024 * 1_024

    return HostConfig.newHostConfig()
      .withNetworkMode("none")
      .withReadonlyRootfs(true)
      .withCapDrop(Capability.ALL)
      .withSecurityOpts(listOf("no-new-privileges"))
      .withNanoCPUs(1_000_000_000)
      .withPidsLimit(128)
      .withMemory(memoryBytes)
      .withMemorySwap(memoryBytes)
      .withUlimits(listOf(Ulimit("nofile", 128L, 128L), Ulimit("fsize", 67_108_864L, 67_108_864L)))
      .withTmpFs(mapOf("/tmp" to "rw,noexec,nosuid,nodev,size=64m"))
      .withLogConfig(LogConfig(LogConfig.LoggingType.NONE))
      .withMounts(listOf(mount, inputMount))
  }

  private fun readExecutionResult(name: String, output: ContainerOutput, timeLimitMs: Int): ProgramResult {
    val state = docker.inspectContainerCmd(name).exec().state
    check(state.status == "exited") { "Container did not finish execution" }
    check(state.error.isNullOrEmpty()) { "Container could not start" }

    val runtimeMs = Duration.between(
      Instant.parse(state.startedAt),
      Instant.parse(state.finishedAt),
    ).toMillis().coerceAtLeast(0)
    val exitCode = checkNotNull(state.exitCodeLong) { "Container exit code is missing" }
    val heapExhausted = exitCode != 0L && listOf(output.stdout, output.stderr).any {
      it.contains("java.lang.OutOfMemoryError")
    }

    val status = when {
      state.oomKilled == true || heapExhausted -> ProgramStatus.MEMORY_LIMIT_EXCEEDED
      exitCode in listOf(124L, 137L) && runtimeMs >= timeLimitMs -> ProgramStatus.TIME_LIMIT_EXCEEDED
      exitCode == 0L -> ProgramStatus.SUCCEEDED
      else -> ProgramStatus.FAILED
    }

    return output.toProgramResult(status, runtimeMs)
  }
}

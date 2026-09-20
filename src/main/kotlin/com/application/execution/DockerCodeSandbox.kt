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
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Runs a program in disposable containers using [runtime], the name of a prebuilt Docker image.
 *
 * 1. Create a workspace: a temporary host directory for one execution's source files and compiled
 *    artifacts. [runTests] owns it; [executionRoot] is the shared parent managed by [DockerExecutionService].
 *
 * 2. Stage the source files and compile them in a container that mounts the workspace at /workspace
 *    with write access. After compilation, remove the source files and retain the compiled artifacts.
 *
 * 3. Write the test inputs as JSON to a separate host file. Mount it read-only in the execution
 *    container and redirect it to stdin.
 *
 * 4. Run the compiled program in a separate container that mounts the workspace read-only.
 *    The Kotlin runtime runs all inputs in one JVM.
 *
 * 5. Capture each case's output in the runtime and emit JSON reports on the container's stdout.
 *    Docker's API streams those reports back for parsing; the grader compares the answers.
 *
 * 6. Remove containers and input files after each step, then delete the workspace in [runTests]'s
 *    finally block, including after staging or compilation failures. The shared parent stays.
 *    After a crash, [containerLabels] let the service find abandoned containers and remove them
 *    before deleting their files.
 */
internal class DockerCodeSandbox(
  private val docker: DockerClient,
  private val runtime: String,
  private val program: PreparedProgram,
  private val executionRoot: Path,
  private val containerLabels: Map<String, String>,
) {
  /** Stages and compiles the program, runs the inputs, and releases execution resources on every exit path. */
  fun runTests(
    inputs: List<JsonElement>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
  ): CodeExecutionResult {
    require(inputs.isNotEmpty())
    require(timeLimitMs in 1..60_000)

    val workspace = Files.createTempDirectory(executionRoot, "submission-")

    try {
      stageSourceFiles(workspace)

      val compilation = compileProgram(workspace)
      if (compilation.status != ProgramStatus.SUCCEEDED) {
        return CodeExecutionResult.CompilationFailed
      }

      return runTestInputs(workspace, inputs, timeLimitMs, memoryLimitMb)
    } finally {
      deleteExecutionWorkspace(workspace)
    }
  }

  private fun stageSourceFiles(workspace: Path) {
    // The compiler writes artifacts into this directory; execution later mounts it read-only.
    Files.setPosixFilePermissions(workspace, PosixFilePermissions.fromString("rwxrwxrwx"))

    for ((name, contents) in program.sourceFiles) {
      val destination = workspace.resolve(name).normalize()
      require(destination.parent == workspace) { "Program sources must use plain file names" }

      Files.writeString(destination, contents)
      Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("r--r--r--"))
    }
  }

  private fun compileProgram(workspace: Path): ProgramResult {
    val command = program.compileCommand ?: return ProgramResult(ProgramStatus.SUCCEEDED)

    val result = runContainer(workspace, command, "", 60_000, 1_024, writable = true)

    if (result.status == ProgramStatus.SUCCEEDED) {
      program.sourceFiles.keys.forEach { Files.deleteIfExists(workspace.resolve(it)) }
    }

    return result
  }

  private fun runTestInputs(
    workspace: Path,
    inputs: List<JsonElement>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
  ): CodeExecutionResult.Completed {
    // Per-case deadlines run inside the JVM. This outer budget also bounds startup and leaked threads.
    val suiteLimitMs = (inputs.size.toLong() * timeLimitMs + 2_000).coerceAtMost(600_000).toInt()
    val input = TestSuiteProtocol.encodeInput(inputs, timeLimitMs)
    // Each case can escape two 20 KB streams, alongside its checkpoint and timing metadata.
    val reportLimit = Math.addExact(1_024, Math.multiplyExact(inputs.size, TestSuiteProtocol.MAX_CASE_RESULT_BYTES))
    val result = runContainer(
      workspace, program.runCommand, input, suiteLimitMs, memoryLimitMb,
      writable = false,
      output = ContainerOutput(reportLimit),
    )

    return readTestSuiteOutput(result, inputs)
  }

  private fun runContainer(
    workspace: Path,
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

      createExecutionContainer(name, workspace, inputFile, command, timeLimitMs, memoryLimitMb, writable)
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
    workspace: Path,
    inputFile: Path,
    command: List<String>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
    writable: Boolean,
  ) {
    val host = createContainerHostConfig(workspace, inputFile, memoryLimitMb, writable)
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
    workspace: Path,
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

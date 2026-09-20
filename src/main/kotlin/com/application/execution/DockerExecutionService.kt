package com.application.execution

import com.application.config.ExecutionProperties
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.DockerException
import kotlinx.serialization.json.JsonElement
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest

/** Manages shared Docker settings, runtime availability, and recovery; delegates each execution to a sandbox. */
@Component
class DockerExecutionService(
  properties: ExecutionProperties,
  private val docker: DockerClient,
) : CodeExecutionService {
  private val executionRoot = Path.of(properties.workspaceDirectory).toAbsolutePath().normalize()

  // Stable labels identify containers belonging to this execution root, including after a restart.
  private val executionRootId = MessageDigest.getInstance("SHA-256")
    .digest(executionRoot.toString().toByteArray())
    .joinToString("") { "%02x".format(it) }
  private val ownershipLabels = mapOf("interview-forge.execution" to executionRootId)

  override fun executeCode(
    executionId: String,
    runtime: String,
    program: PreparedProgram,
    inputs: List<JsonElement>,
    timeLimitMs: Int,
    memoryLimitMb: Int,
  ): CodeExecutionResult {
    val sandbox = createSandbox(executionId, runtime, program)

    return sandbox.runTests(inputs, timeLimitMs, memoryLimitMb)
  }

  override fun isAvailable(runtime: String): Boolean {
    if (runtime.isBlank()) {
      return false
    }

    return try {
      // The runtime names a local Docker image. Inspection neither downloads it nor starts a container.
      docker.inspectImageCmd(runtime).exec()
      true
    } catch (_: DockerException) {
      false
    } catch (exception: RuntimeException) {
      if (exception.cause !is IOException) {
        throw exception
      }

      false
    }
  }

  /** Configures a sandbox under the shared root; runTests creates and owns its execution workspace. */
  internal fun createSandbox(executionId: String, runtime: String, program: PreparedProgram): DockerCodeSandbox {
    require(executionId.isNotBlank())
    require(runtime.isNotBlank())
    require(program.runCommand.isNotEmpty())

    Files.createDirectories(executionRoot)
    Files.setPosixFilePermissions(executionRoot, PosixFilePermissions.fromString("rwx------"))

    return DockerCodeSandbox(
      docker = docker,
      runtime = runtime,
      program = program,
      executionRoot = executionRoot,
      containerLabels = ownershipLabels + ("interview-forge.execution-id" to executionId),
    )
  }

  override fun cleanUpInterruptedExecutions() {
    // Include stopped containers: Docker retains them until removal. Labels scope cleanup to this execution root.
    val containers = docker.listContainersCmd()
      .withShowAll(true)
      .withLabelFilter(ownershipLabels)
      .exec()

    // Force-remove leftover containers before deleting files they may still be using.
    for (container in containers) {
      docker.removeExecutionContainer(container.id)
    }

    if (Files.isDirectory(executionRoot)) {
      Files.list(executionRoot).use { paths ->
        paths.filter { it.fileName.toString().startsWith("submission-") }.forEach(::deleteExecutionWorkspace)
      }
    }
  }
}

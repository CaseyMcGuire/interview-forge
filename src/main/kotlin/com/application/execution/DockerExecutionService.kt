package com.application.execution

import com.application.config.ExecutionProperties
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.DockerException
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest

/**
 * Stages source files on the host, then hands them to a runner that compiles and runs the test suite
 * in disposable containers. Each container starts from the same prebuilt Docker image.
 */
@Component
class DockerExecutionService(
  properties: ExecutionProperties,
  private val docker: DockerClient,
) : ProgramExecutor {
  private val workspaceRoot = Path.of(properties.workspaceDirectory).toAbsolutePath().normalize()

  // Stable labels identify this workspace's containers, including those left behind after a restart.
  private val workspaceId = MessageDigest.getInstance("SHA-256")
    .digest(workspaceRoot.toString().toByteArray())
    .joinToString("") { "%02x".format(it) }
  private val workspaceLabels = mapOf("interview-forge.execution" to workspaceId)

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

  /**
   * Writes the program's source files into a temporary host directory with permissions for the compiler container.
   * Returns a runner configured with the Docker image and submission labels. The caller compiles and runs
   * the suite through that runner, then closes it to delete the directory. If preparation fails, this method
   * deletes the directory before propagating the error.
   */
  override fun prepareProgram(submissionId: Long, runtime: String, program: PreparedProgram): ProgramExecution {
    require(submissionId > 0)
    require(runtime.isNotBlank())
    require(program.runCommand.isNotEmpty())

    Files.createDirectories(workspaceRoot)
    Files.setPosixFilePermissions(workspaceRoot, PosixFilePermissions.fromString("rwx------"))

    // Both containers mount this host directory at /workspace: compilation writes the artifacts,
    // then the suite reads them through a read-only mount.
    val workspace = Files.createTempDirectory(workspaceRoot, "submission-")

    try {
      // Only this disposable directory is writable by the unprivileged compiler container.
      Files.setPosixFilePermissions(workspace, PosixFilePermissions.fromString("rwxrwxrwx"))
      for ((name, contents) in program.sourceFiles) {
        val destination = workspace.resolve(name).normalize()
        require(destination.parent == workspace) { "Program sources must use plain file names" }
        Files.writeString(destination, contents)
        Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("r--r--r--"))
      }

      // Containers start when compileProgram/runTestSuite are called. Closing the runner deletes the workspace.
      return DockerSubmissionRunner(
        docker = docker,
        runtime = runtime,
        program = program,
        workspace = workspace,
        containerLabels = workspaceLabels + ("interview-forge.submission" to submissionId.toString()),
      )
    } catch (exception: Exception) {
      deleteExecutionWorkspace(workspace)
      throw exception
    }
  }

  override fun cleanUpInterruptedExecutions() {
    // Include stopped containers: Docker retains them until removal. Labels keep cleanup scoped to this workspace.
    val containers = docker.listContainersCmd()
      .withShowAll(true)
      .withLabelFilter(workspaceLabels)
      .exec()

    // Force-remove leftover containers before deleting files they may still be using.
    for (container in containers) {
      docker.removeExecutionContainer(container.id)
    }

    if (Files.isDirectory(workspaceRoot)) {
      Files.list(workspaceRoot).use { paths ->
        paths.filter { it.fileName.toString().startsWith("submission-") }.forEach(::deleteExecutionWorkspace)
      }
    }
  }
}

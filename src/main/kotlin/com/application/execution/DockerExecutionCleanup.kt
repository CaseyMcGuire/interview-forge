package com.application.execution

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import java.nio.file.Files
import java.nio.file.Path

internal fun DockerClient.removeExecutionContainer(name: String) {
  // Preserve cancellation, but allow container removal to finish before restoring the flag.
  val interrupted = Thread.interrupted()

  try {
    removeContainerCmd(name).withForce(true).exec()
  } catch (_: NotFoundException) {
    // Creation may have failed before Docker allocated the container.
  } finally {
    if (interrupted) {
      Thread.currentThread().interrupt()
    }
  }
}

internal fun deleteExecutionWorkspace(workspace: Path) {
  // Files.walk does not follow symlinks created inside the workspace.
  Files.walk(workspace).use { paths ->
    paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
  }
}

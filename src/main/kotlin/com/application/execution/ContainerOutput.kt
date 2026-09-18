package com.application.execution

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import java.io.ByteArrayOutputStream

/** Closes the Docker output stream when either stream exceeds its independent byte limit. */
internal class ContainerOutput(
  private val maxStdoutBytes: Int = 20_000,
) : ResultCallback.Adapter<Frame>() {
  private val stdoutBuffer = ByteArrayOutputStream()
  private val stderrBuffer = ByteArrayOutputStream()

  val stdout: String
    @Synchronized get() = stdoutBuffer.toString(Charsets.UTF_8)

  val stderr: String
    @Synchronized get() = stderrBuffer.toString(Charsets.UTF_8)

  @Volatile
  var limitExceeded = false
    private set

  @Synchronized
  override fun onNext(frame: Frame) {
    val output = when (frame.streamType) {
      StreamType.STDOUT -> stdoutBuffer
      StreamType.STDERR -> stderrBuffer
      else -> error("Unexpected Docker output stream: ${frame.streamType}")
    }
    val limit = if (frame.streamType == StreamType.STDOUT) maxStdoutBytes else 20_000

    val bytesToRetain = minOf(frame.payload.size, limit - output.size())
    output.write(frame.payload, 0, bytesToRetain)

    if (bytesToRetain < frame.payload.size) {
      limitExceeded = true
      close()
    }
  }

  @Synchronized
  fun toProgramResult(status: ProgramStatus, runtimeMs: Long? = null) = ProgramResult(
    status = status,
    stdout = stdout,
    stderr = stderr,
    runtimeMs = runtimeMs,
  )
}

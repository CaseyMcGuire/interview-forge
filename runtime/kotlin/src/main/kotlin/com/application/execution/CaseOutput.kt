package com.application.execution

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Bounds each case's output even though the JVM lives for the entire suite. */
internal class CaseOutput : OutputStream() {
  private val bytes = ByteArrayOutputStream()

  @Volatile
  var limitExceeded = false
    private set

  override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)

  @Synchronized
  override fun write(source: ByteArray, offset: Int, length: Int) {
    val retained = minOf(length, TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES - bytes.size())
    bytes.write(source, offset, retained)

    if (retained < length) {
      limitExceeded = true
      throw OutputLimitExceeded()
    }
  }

  @Synchronized
  fun text(): String = boundedCaseOutput(bytes.toString(Charsets.UTF_8))
}

internal class OutputLimitExceeded : RuntimeException()

/** Decoding partial or invalid UTF-8 can expand replacement characters beyond the original byte cap. */
fun boundedCaseOutput(text: String): String {
  val bytes = text.toByteArray(Charsets.UTF_8)
  if (bytes.size <= TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES) {
    return text
  }

  return Charsets.UTF_8.newDecoder()
    .onMalformedInput(CodingErrorAction.IGNORE)
    .decode(ByteBuffer.wrap(bytes, 0, TestSuiteProtocol.MAX_CASE_OUTPUT_BYTES))
    .toString()
}

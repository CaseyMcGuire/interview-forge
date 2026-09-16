package com.application.execution

/** Provided by the execution backend; absent until an executor is installed. */
fun interface RuntimeAvailability {
  /** Read-only check that the configured runtime can accept work. */
  fun isAvailable(runtime: String): Boolean
}

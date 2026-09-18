package com.application.execution

/** Checks whether the configured execution environment is installed and reachable. */
fun interface RuntimeAvailability {
  /** Read-only check that the configured runtime can accept work. */
  fun isAvailable(runtime: String): Boolean
}

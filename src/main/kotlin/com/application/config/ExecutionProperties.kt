package com.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** One execution environment per language key, shared by all problems in that language. */
@ConfigurationProperties("execution")
data class ExecutionProperties(
  val runtimes: Map<String, String> = emptyMap(),

  /** Combined queued/running problem submissions and custom input submissions allowed across all users. */
  val maxActiveSubmissions: Int = 100,

  /** Combined queued/running attempts allowed for one user. */
  val maxActiveSubmissionsPerUser: Int = 2,

  /** Maximum number of user-supplied test cases accepted in one custom run. */
  val maxCustomTestCases: Int = 20,

  /** Retention from custom input submission creation; cleanup waits for execution to finish. */
  val customInputSubmissionLifetime: Duration = Duration.ofMinutes(5),

  val workerEnabled: Boolean = true,
  val workspaceDirectory: String = "${System.getProperty("java.io.tmpdir")}/interview-forge-execution",
) {
  init {
    require(maxActiveSubmissions in 1..1000)
    require(maxActiveSubmissionsPerUser in 1..maxActiveSubmissions)
    require(maxCustomTestCases > 0)
    require(!customInputSubmissionLifetime.isNegative && !customInputSubmissionLifetime.isZero)
  }
}

package com.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** One execution environment per language key, shared by all problems in that language. */
@ConfigurationProperties("execution")
data class ExecutionProperties(
  val runtimes: Map<String, String> = emptyMap(),
  val maxActiveSubmissions: Int = 100,
  val maxActiveSubmissionsPerUser: Int = 2,
  val workerEnabled: Boolean = true,
  val workspaceDirectory: String = "${System.getProperty("java.io.tmpdir")}/interview-forge-execution",
) {
  init {
    require(maxActiveSubmissions in 1..1000)
    require(maxActiveSubmissionsPerUser in 1..maxActiveSubmissions)
  }
}

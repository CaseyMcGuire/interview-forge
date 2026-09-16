package com.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** One execution environment per language key, shared by all problems in that language. */
@ConfigurationProperties("execution")
data class ExecutionProperties(
  val runtimes: Map<String, String> = emptyMap(),
)

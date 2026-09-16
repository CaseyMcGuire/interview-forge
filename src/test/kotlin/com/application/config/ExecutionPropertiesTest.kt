package com.application.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ExecutionPropertiesTest {
  @Test
  fun `binds a runtime for each configured language`() {
    ApplicationContextRunner()
      .withUserConfiguration(PropertyBindingConfiguration::class.java)
      .withPropertyValues(
        "execution.runtimes.kotlin=kotlin-2.1",
        "execution.runtimes.python=python-3.13",
      )
      .run { context ->
        assertEquals(
          mapOf("kotlin" to "kotlin-2.1", "python" to "python-3.13"),
          context.getBean(ExecutionProperties::class.java).runtimes,
        )
      }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ExecutionProperties::class)
  class PropertyBindingConfiguration
}

package com.application.config

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class DockerConfiguration {
  @Bean(destroyMethod = "close")
  fun dockerClient(): DockerClient {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    val transport = ApacheDockerHttpClient.Builder()
      .dockerHost(config.dockerHost)
      .sslConfig(config.sslConfig)
      .maxConnections(100)
      .connectionTimeout(Duration.ofSeconds(2))
      // A compiler may stay silent for its full 60-second budget while output is attached.
      .responseTimeout(Duration.ofSeconds(70))
      .build()

    return DockerClientImpl.getInstance(config, transport)
  }
}

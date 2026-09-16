package com.application

import com.application.config.ExecutionProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ExecutionProperties::class)
open class MainApplication

fun main(args: Array<String>) {
  runApplication<MainApplication>(*args)
}

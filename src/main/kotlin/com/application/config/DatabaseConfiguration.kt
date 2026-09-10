package com.application.config

import com.application.db.policies.UserPolicy
import com.application.ent.EntClient
import entkt.postgres.PostgresDriver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {
  @Bean
  fun entClient(dataSource: DataSource): EntClient {
    // Flyway owns database changes; registering EntKt schemas only configures runtime metadata.
    return EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies {
        users(UserPolicy)
      }
    }
  }
}

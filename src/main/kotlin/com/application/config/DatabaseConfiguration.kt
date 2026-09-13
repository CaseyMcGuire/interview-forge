package com.application.config

import com.application.db.policies.LanguagePolicy
import com.application.db.policies.ProblemLanguagePolicy
import com.application.db.policies.ProblemPolicy
import com.application.db.policies.TestCasePolicy
import com.application.db.policies.UserPolicy
import com.application.ent.EntClient
import entkt.postgres.PostgresDriver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {
  @Bean
  fun entClient(
    dataSource: DataSource,
    userPolicy: UserPolicy,
    problemPolicy: ProblemPolicy,
    languagePolicy: LanguagePolicy,
    problemLanguagePolicy: ProblemLanguagePolicy,
    testCasePolicy: TestCasePolicy,
  ): EntClient {
    // Flyway owns database changes; registering EntKt schemas only configures runtime metadata.
    return EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies {
        users(userPolicy)
        problems(problemPolicy)
        languages(languagePolicy)
        problemLanguages(problemLanguagePolicy)
        testCases(testCasePolicy)
      }
    }
  }
}

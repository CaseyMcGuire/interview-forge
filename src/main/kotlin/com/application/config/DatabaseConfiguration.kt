package com.application.config

import com.application.db.policies.CustomTestCasePolicy
import com.application.db.policies.CustomTestSuitePolicy
import com.application.db.policies.CustomTestSuiteRunPolicy
import com.application.db.policies.JudgeConfigurationPolicy
import com.application.db.policies.LanguagePolicy
import com.application.db.policies.ProblemLanguagePolicy
import com.application.db.policies.ProblemPolicy
import com.application.db.policies.TestCasePolicy
import com.application.db.policies.UserPolicy
import com.application.db.policies.SubmissionPolicy
import com.application.db.policies.SubmissionFailurePolicy
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
    judgeConfigurationPolicy: JudgeConfigurationPolicy,
    submissionPolicy: SubmissionPolicy,
    submissionFailurePolicy: SubmissionFailurePolicy,
    customTestSuitePolicy: CustomTestSuitePolicy,
    customTestCasePolicy: CustomTestCasePolicy,
    customTestSuiteRunPolicy: CustomTestSuiteRunPolicy,
  ): EntClient {
    // Flyway owns database changes; registering EntKt schemas only configures runtime metadata.
    return EntClient(PostgresDriver(dataSource, autoDdl = false)) {
      policies {
        users(userPolicy)
        problems(problemPolicy)
        languages(languagePolicy)
        problemLanguages(problemLanguagePolicy)
        testCases(testCasePolicy)
        judgeConfigurations(judgeConfigurationPolicy)
        submissions(submissionPolicy)
        submissionFailures(submissionFailurePolicy)
        customTestSuites(customTestSuitePolicy)
        customTestCases(customTestCasePolicy)
        customTestSuiteRuns(customTestSuiteRunPolicy)
      }
    }
  }
}

package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonArray

/** One custom execution attempt; passing its cases does not establish an official acceptance. */
class CustomTestSuiteRun : EntSchema("custom_test_suite_runs", clientName = "customTestSuiteRuns") {
  override fun id() = EntId.long()

  /** Derived from the authenticated user when the run is created. */
  val user by belongsTo<User>("user")
    .immutable()
    .onDelete(OnDelete.RESTRICT)

  /** Determines the problem and language for reference preparation and grading. */
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language")
    .immutable()
    .inverse(ProblemLanguage::customTestSuiteRuns)
    .onDelete(OnDelete.RESTRICT)

  val cases by hasMany<CustomTestCase>("cases")

  /** Cleanup may delete the run and its cases after this time, once execution finishes. */
  val expiresAt by instant("expires_at").immutable()

  val execution = include(::ExecutionAttemptFields)

  val status by enum<CustomTestSuiteRunStatus>("status").default(CustomTestSuiteRunStatus.QUEUED)
  val outcome by enum<CustomTestSuiteRunOutcome>("outcome").nullable()

  /**
   * Null until completion, then one result per case, including NOT_RUN for unexecuted cases.
   * Entries reference custom test case IDs; input, expectation, and position stay on the case.
   */
  val caseResults by json<JsonArray>("case_results").nullable()

  /** Created after reference outputs are prepared; removed when the result is saved. */
  val gradingJob by hasOne<GradingJob>("grading_job")

  val timestamps = include(::Timestamps)

  /** Supports active counts and oldest-first worker claims. */
  val byStatusAndCreatedAt = index("idx_custom_test_suite_runs_status_created_at", status, timestamps.createdAt)

  val byUser = index("idx_custom_test_suite_runs_user", user.fk)
  val byProblemLanguage = index("idx_custom_test_suite_runs_problem_language", problemLanguage.fk)
  val byExpiresAt = index("idx_custom_test_suite_runs_expires_at", expiresAt)
}

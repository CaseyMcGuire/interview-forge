package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonArray

/** One custom execution attempt; passing its cases does not establish an official acceptance. */
class CustomTestSuiteRun : EntSchema("custom_test_suite_runs", clientName = "customTestSuiteRuns") {
  override fun id() = EntId.long()

  /** Each run has its own suite, which determines its owner, problem, language, and inputs. */
  val customTestSuite by belongsTo<CustomTestSuite>("custom_test_suite")
    .immutable()
    .unique()
    .inverse(CustomTestSuite::run)
    .onDelete(OnDelete.CASCADE)

  val execution = include(::ExecutionAttemptFields)

  val status by enum<CustomTestSuiteRunStatus>("status").default(CustomTestSuiteRunStatus.QUEUED)
  val outcome by enum<CustomTestSuiteRunOutcome>("outcome").nullable()

  /**
   * Null until completion, then one result per case, including NOT_RUN for unexecuted cases.
   * Entries reference custom test case IDs; input, expectation, and position stay on the case.
   */
  val caseResults by json<JsonArray>("case_results").nullable()

  val timestamps = include(::Timestamps)

  /** Supports active counts and oldest-first worker claims. */
  val byStatusAndCreatedAt = index("idx_custom_test_suite_runs_status_created_at", status, timestamps.createdAt)
}

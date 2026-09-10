package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/* A selected test's immutable input snapshot plus its eventual execution result. */
class SubmissionTestResult : EntSchema("submission_test_results", clientName = "submissionTestResults") {
  /* Database-generated identity for one case within one attempt. */
  override fun id() = EntId.long()

  /* Attempt that owns the snapshot and outcome; cases cannot move between submissions. */
  val submissionId by long("submission_id").immutable()

  /* Link to the owning attempt; results remain part of its retained history. */
  val submission by belongsTo<Submission>("submission")
    .field(submissionId)
    .inverse(Submission::testResults)
    .onDelete(OnDelete.RESTRICT)

  /* Optional official-test provenance; null for custom inputs or when the original test is deleted. */
  val testCaseId by long("test_case_id").nullable()

  /* Deleting an official test clears only this reference, leaving all snapshot fields intact. */
  val testCase by belongsTo<TestCase>("test_case")
    .field(testCaseId)
    .nullable()
    .onDelete(OnDelete.SET_NULL)

  /* Nonnegative case order within the attempt, fixed when the execution inputs are selected. */
  val position by int("position").immutable()

  /* Original example/hidden/custom classification; later edits must not expose a formerly hidden case. */
  val source by enum<SubmissionTestSource>("source").immutable()

  /* Exact input selected for this run; grading and history must use this snapshot, not the current test. */
  val inputJson by json<JsonElement>("input_json").immutable().sensitive()

  /* Snapshotted expectation; SQL null means no custom expectation, while JSON null is an expected value. */
  val expectedOutputJson by json<JsonElement>("expected_output_json")
    .nullable()
    .immutable()
    .sensitive()

  /* Initially pending; EXECUTED means a custom case returned without an expected-output comparison. */
  val outcome by enum<SubmissionTestOutcome>("outcome").default(SubmissionTestOutcome.PENDING)

  /* Serialized return value; SQL null means no captured result, while JSON null is a returned value. */
  val actualOutputJson by json<JsonElement>("actual_output_json").nullable().sensitive()

  /* Captured standard output; it can reveal hidden inputs and must follow the case's access restrictions. */
  val stdout by text("stdout").nullable().sensitive()

  /* Captured standard error; potentially contains private test or harness details. */
  val stderr by text("stderr").nullable().sensitive()

  /* Measured execution time for this case in milliseconds; null when it was not measured. */
  val runtimeMs by long("runtime_ms").nullable()

  /* Peak measured memory for this case in megabytes; null when it was not measured. */
  val peakMemoryMb by int("peak_memory_mb").nullable()

  /* Time the case snapshot was recorded, before its execution starts. */
  val createdAt by time("created_at").defaultNow().immutable()

  /* Time the outcome, captured output, or execution measurements were last updated. */
  val updatedAt by time("updated_at").defaultNow().updateDefaultNow()

  /* Prevents multiple case rows from occupying the same position in one submission. */
  val bySubmissionAndPosition =
    index("uq_submission_test_results_submission_position", submissionId, position).unique()

  /* Supports provenance lookup and clearing references when an official test is deleted. */
  val byTestCase = index("idx_submission_test_results_test_case", testCaseId)
}

package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/** The first failed official case; successful cases are not retained by the submission worker. */
class ProblemSubmissionFailure : EntSchema("problem_submission_failures", clientName = "problemSubmissionFailures") {
  /** Database-generated identity for the retained failure. */
  override fun id() = EntId.long()

  val problemSubmission by belongsTo<ProblemSubmission>("problem_submission_id")
    .immutable()
    .unique()
    .inverse(ProblemSubmission::failedTestResult)
    .onDelete(OnDelete.RESTRICT)
    .comment("Each submission retains at most one failure; its snapshot cannot move between submissions.")

  val testCase by belongsTo<TestCase>("test_case_id")
    .nullable()
    .onDelete(OnDelete.SET_NULL)
    .comment("Optional official-test provenance; deletion clears this reference while preserving the snapshot.")

  val source by enum<ProblemSubmissionTestSource>("source").immutable()
    .comment("Original example/hidden/custom classification; later edits must not expose a formerly hidden case.")

  val inputJson by json<JsonElement>("input_json").immutable().sensitive()
    .comment("Exact input selected for this run; grading and history must use this snapshot, not the current test.")

  val expectedOutputJson by json<JsonElement>("expected_output_json")
    .nullable()
    .immutable()
    .sensitive()
    .comment("Snapshotted expectation; SQL null means no custom expectation, while JSON null is an expected value.")

  val outcome by enum<ProblemSubmissionTestOutcome>("outcome").default(ProblemSubmissionTestOutcome.PENDING)
    .comment("The worker writes a terminal failure outcome when recording this row.")

  val actualOutputJson by json<JsonElement>("actual_output_json").nullable().sensitive()
    .comment("Serialized return value; SQL null means no captured result, while JSON null is a returned value.")

  val stdout by string("stdout").nullable().sensitive()
    .comment("Captured standard output; it can reveal hidden inputs and must follow the case's access restrictions.")

  val stderr by string("stderr").nullable().sensitive()
    .comment("Captured standard error; potentially contains private test or harness details.")

  val runtimeMs by long("runtime_ms").nullable()
    .comment("Measured execution time for this case in milliseconds; null when it was not measured.")

  val peakMemoryMb by int("peak_memory_mb").nullable()
    .comment("Peak measured memory for this case in megabytes; null when it was not measured.")

  val createdAt by instant("created_at").defaultNow().immutable()
    .comment("Time the first failed case was retained, after execution.")

  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
    .comment("Time the outcome, captured output, or execution measurements were last updated.")

  /** Supports provenance lookup and clearing references when an official test is deleted. */
  val byTestCase by index("idx_problem_submission_failures_test_case", testCase.fk)
}

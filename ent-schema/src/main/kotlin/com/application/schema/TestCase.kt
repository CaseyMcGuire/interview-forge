package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/** An editable official test shared across languages; submission results snapshot its contents. */
class TestCase : EntSchema("test_cases", clientName = "testCases") {
  /** Database-generated identity used for optional provenance in historical results. */
  override fun id() = EntId.long()

  val problem by belongsTo<Problem>("problem_id")
    .immutable()
    .inverse(Problem::testCases)
    .onDelete(OnDelete.RESTRICT)
    .comment("Fixed problem whose input contract and checking rules this official test follows.")

  val position by int("position")
    .comment("Nonnegative display/execution order, unique within the problem's official test suite.")

  val visibility by enum<TestCaseVisibility>("visibility")
    .comment("EXAMPLE may be shown before solving; HIDDEN input/output must remain private to grading.")

  val inputJson by json<JsonElement>("input_json").sensitive()
    .comment("Language-independent args or constructor/call sequence; redacted because cases may be hidden.")

  val expectedOutputJson by json<JsonElement>("expected_output_json").sensitive()
    .comment("Expected result or custom-checker reference data; JSON null is a value, not a missing expectation.")

  val explanationMarkdown by string("explanation_markdown").nullable().sensitive()
    .comment("Optional explanation of the case; hidden-case explanations need the same access restrictions.")

  val timestamps = include(::Timestamps)

  /** Provides deterministic test ordering without duplicate positions inside one problem. */
  val byProblemAndPosition by index("uq_test_cases_problem_position", problem.fk, position).unique()
}

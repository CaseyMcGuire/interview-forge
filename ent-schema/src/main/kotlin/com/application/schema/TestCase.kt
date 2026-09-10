package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/* An editable official test shared across languages; submission results snapshot its contents. */
class TestCase : EntSchema("test_cases", clientName = "testCases") {
  /* Database-generated identity used for optional provenance in historical results. */
  override fun id() = EntId.long()

  /* Problem whose input contract and checking rules this test follows. */
  val problemId by long("problem_id").immutable()

  /* Link to the shared problem's official test suite. */
  val problem by belongsTo<Problem>("problem")
    .field(problemId)
    .inverse(Problem::testCases)
    .onDelete(OnDelete.RESTRICT)

  /* Nonnegative display/execution order, unique within the problem's official test suite. */
  val position by int("position")

  /* EXAMPLE may be shown before solving; HIDDEN input/output must remain private to grading. */
  val visibility by enum<TestCaseVisibility>("visibility")

  /* Language-independent args or constructor/call sequence; redacted because cases may be hidden. */
  val inputJson by json<JsonElement>("input_json").sensitive()

  /* Expected result or custom-checker reference data; JSON null is a value, not a missing expectation. */
  val expectedOutputJson by json<JsonElement>("expected_output_json").sensitive()

  /* Optional explanation of the case; hidden-case explanations need the same access restrictions. */
  val explanationMarkdown by text("explanation_markdown").nullable().sensitive()

  /* Time this official test was created. */
  val createdAt by time("created_at").defaultNow().immutable()

  /* Time its input, expectation, visibility, explanation, or position was last edited. */
  val updatedAt by time("updated_at").defaultNow().updateDefaultNow()

  /* Provides deterministic test ordering without duplicate positions inside one problem. */
  val byProblemAndPosition = index("uq_test_cases_problem_position", problemId, position).unique()
}

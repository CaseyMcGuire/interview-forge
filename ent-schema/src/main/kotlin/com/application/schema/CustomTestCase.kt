package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/** A fixed input with an expectation prepared by the selected language's reference solution. */
class CustomTestCase : EntSchema("custom_test_cases", clientName = "customTestCases") {
  override fun id() = EntId.long()

  val customInputSubmission by belongsTo<CustomInputSubmission>("custom_input_submission")
    .immutable()
    .inverse(CustomInputSubmission::cases)
    .onDelete(OnDelete.CASCADE)

  /** Zero-based position in the submitted input list; results refer back to this case. */
  val position by int("position").immutable()

  val inputJson by json<JsonElement>("input_json").immutable()

  /** SQL null means unprepared; JSON null is a prepared answer. Save all expectations together. */
  val expectedOutputJson by json<JsonElement>("expected_output_json").nullable()

  val timestamps = include(::Timestamps)

  val bySubmissionAndPosition =
    index("uq_custom_test_cases_submission_position", customInputSubmission.fk, position).unique()
}

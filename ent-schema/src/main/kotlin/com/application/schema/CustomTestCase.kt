package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/** A fixed input with an expectation prepared by the selected language's reference solution. */
class CustomTestCase : EntSchema("custom_test_cases", clientName = "customTestCases") {
  override fun id() = EntId.long()

  val customInputSubmission by belongsTo<CustomInputSubmission>("custom_input_submission_id")
    .immutable()
    .inverse(CustomInputSubmission::cases)
    .onDelete(OnDelete.CASCADE)

  val position by int("position").immutable()
    .comment("Zero-based position in the submitted input list; results refer back to this case.")

  val inputJson by json<JsonElement>("input_json").immutable()

  val expectedOutputJson by json<JsonElement>("expected_output_json").nullable()
    .comment("SQL null means unprepared; JSON null is a prepared answer. Save all expectations together.")

  val timestamps = include(::Timestamps)

  val bySubmissionAndPosition by
    index("uq_custom_test_cases_submission_position", customInputSubmission.fk, position).unique()
}

package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonElement

/** A fixed input with an expectation prepared by the selected language's reference solution. */
class CustomTestCase : EntSchema("custom_test_cases", clientName = "customTestCases") {
  override fun id() = EntId.long()

  val customTestSuite by belongsTo<CustomTestSuite>("custom_test_suite")
    .immutable()
    .inverse(CustomTestSuite::cases)
    .onDelete(OnDelete.CASCADE)

  /** Zero-based position in the submitted input list; results refer back to this case. */
  val position by int("position").immutable()

  val inputJson by json<JsonElement>("input_json").immutable().sensitive()

  /** SQL null means unprepared; JSON null is a prepared answer. Save all expectations together. */
  val expectedOutputJson by json<JsonElement>("expected_output_json").nullable().sensitive()

  val timestamps = include(::Timestamps)

  val bySuiteAndPosition =
    index("uq_custom_test_cases_suite_position", customTestSuite.fk, position).unique()
}

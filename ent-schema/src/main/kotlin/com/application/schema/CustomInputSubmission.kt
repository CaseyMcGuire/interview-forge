package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete
import kotlinx.serialization.json.JsonArray

/** One custom execution attempt; passing its cases does not establish an official acceptance. */
class CustomInputSubmission : EntSchema("custom_input_submissions", clientName = "customInputSubmissions") {
  override fun id() = EntId.long()

  val user by belongsTo<User>("user_id")
    .immutable()
    .onDelete(OnDelete.RESTRICT)
    .comment("Derived from the authenticated user when the run is created.")

  val problemLanguage by belongsTo<ProblemLanguage>("problem_language_id")
    .immutable()
    .inverse(ProblemLanguage::customInputSubmissions)
    .onDelete(OnDelete.RESTRICT)
    .comment("Determines the problem and language for reference preparation and grading.")

  val cases by hasMany<CustomTestCase>()

  val expiresAt by instant("expires_at").immutable()
    .comment("Cleanup may delete the run and its cases after this time, once execution finishes.")

  val execution = include(::ExecutionAttemptFields)

  val status by enum<CustomInputSubmissionStatus>("status").default(CustomInputSubmissionStatus.QUEUED)
  val outcome by enum<CustomInputSubmissionOutcome>("outcome").nullable()

  val caseResults by json<JsonArray>("case_results").nullable()
    .comment(
      """
      Null until completion, then one result per case, including NOT_RUN for unexecuted cases.
      Entries reference custom test case IDs; input, expectation, and position stay on the case.
      """.trimIndent(),
    )

  val gradingJob by hasOne<GradingJob>()
    .comment("Created after reference outputs are prepared; removed when the result is saved.")

  val timestamps = include(::Timestamps)

  /** Supports active counts and oldest-first worker claims. */
  val byStatusAndCreatedAt by index("idx_custom_input_submissions_status_created_at", status, timestamps.createdAt)

  val byUser by index("idx_custom_input_submissions_user", user.fk)
  val byProblemLanguage by index("idx_custom_input_submissions_problem_language", problemLanguage.fk)
  val byExpiresAt by index("idx_custom_input_submissions_expires_at", expiresAt)
}

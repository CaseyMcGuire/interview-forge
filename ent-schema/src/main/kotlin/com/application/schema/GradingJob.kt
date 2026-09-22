package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Ready-to-grade work, deleted in the transaction that saves the originating attempt's result. */
class GradingJob : EntSchema("grading_jobs", clientName = "gradingJobs") {
  override fun id() = EntId.long()

  /** Exactly one of problemSubmission and customInputSubmission identifies the result destination. */
  val problemSubmission by belongsTo<ProblemSubmission>("problem_submission_id")
    .nullable()
    .immutable()
    .unique()
    .inverse(ProblemSubmission::gradingJob)
    .onDelete(OnDelete.CASCADE)

  val customInputSubmission by belongsTo<CustomInputSubmission>("custom_input_submission_id")
    .nullable()
    .immutable()
    .unique()
    .inverse(CustomInputSubmission::gradingJob)
    .onDelete(OnDelete.CASCADE)

  /** Selects the language and current judge configuration; the runtime stays in application config. */
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language_id")
    .immutable()
    .onDelete(OnDelete.RESTRICT)

  val sourceCode by string("source_code").immutable()

  /**
   * Ordered input/expected-output snapshots, with case IDs and official-case visibility for result storage.
   * Expected outputs are present before enqueueing, including JSON null when that is the answer.
   */
  val cases by json<List<GradingCase>>("cases").immutable()

  val status by enum<GradingJobStatus>("status").default(GradingJobStatus.QUEUED)
  val createdAt by instant("created_at").defaultNow().immutable()
  val startedAt by instant("started_at").nullable()

  val byStatusAndCreatedAt = index("idx_grading_jobs_status_created_at", status, createdAt)
  val byProblemLanguage = index("idx_grading_jobs_problem_language", problemLanguage.fk)
}

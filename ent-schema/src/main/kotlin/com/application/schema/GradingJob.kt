package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Ready-to-grade work, deleted in the transaction that saves the originating attempt's result. */
class GradingJob : EntSchema("grading_jobs", clientName = "gradingJobs") {
  override fun id() = EntId.long()

  /** Exactly one of submission and customTestSuiteRun identifies the result destination. */
  val submission by belongsTo<Submission>("submission")
    .nullable()
    .immutable()
    .unique()
    .inverse(Submission::gradingJob)
    .onDelete(OnDelete.CASCADE)

  val customTestSuiteRun by belongsTo<CustomTestSuiteRun>("custom_test_suite_run")
    .nullable()
    .immutable()
    .unique()
    .inverse(CustomTestSuiteRun::gradingJob)
    .onDelete(OnDelete.CASCADE)

  /** Selects the language and current judge configuration; the runtime stays in application config. */
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language")
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

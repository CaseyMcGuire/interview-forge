package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** One user's retained code snapshot and official submission summary. */
class ProblemSubmission : EntSchema("problem_submissions", clientName = "problemSubmissions") {
  /** Database-generated identity for a single attempt; running again creates another submission. */
  override fun id() = EntId.long()

  /** Fixed owner of this retained attempt; future writes must derive it from the authenticated user. */
  val user by belongsTo<User>("user_id")
    .immutable()
    .onDelete(OnDelete.RESTRICT)

  /** Fixed problem for history filtering; must match the selected problem-language configuration. */
  val problem by belongsTo<Problem>("problem_id")
    .immutable()
    .inverse(Problem::problemSubmissions)
    .onDelete(OnDelete.RESTRICT)

  /** Fixed language configuration; its current harness and stencil may differ from those used here. */
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language_id")
    .immutable()
    .inverse(ProblemLanguage::problemSubmissions)
    .onDelete(OnDelete.RESTRICT)

  val execution = include(::ExecutionAttemptFields)

  /** Execution lifecycle, initially queued; only a trusted grading path should advance it. */
  val status by enum<ProblemSubmissionStatus>("status").default(ProblemSubmissionStatus.QUEUED)

  /** PENDING until execution finishes; ACCEPTED means all official tests passed. */
  val verdict by enum<ProblemSubmissionVerdict>("verdict").default(ProblemSubmissionVerdict.PENDING)

  /** Highest measured memory use among executed cases, in megabytes; null if unavailable. */
  val peakMemoryMb by int("peak_memory_mb").nullable()

  /** The first failed case, when known; hidden-case details remain private to execution. */
  val failedTestResult by hasOne<ProblemSubmissionFailure>()

  /** Present while the submitted source is waiting for grading or being graded. */
  val gradingJob by hasOne<GradingJob>()

  val timestamps = include(::Timestamps)

  /** Supports a user's complete submission history. */
  val byUserAndCreatedAt = index("idx_problem_submissions_user_created_at", user.fk, timestamps.createdAt)

  /** Supports a user's attempts and solved-state queries for one problem. */
  val byUserProblemAndCreatedAt =
    index("idx_problem_submissions_user_problem_created_at", user.fk, problem.fk, timestamps.createdAt)

  /** Supports problem-level history and the problem foreign key. */
  val byProblem = index("idx_problem_submissions_problem", problem.fk)

  /** Supports language-configuration history and its foreign key. */
  val byProblemLanguage = index("idx_problem_submissions_problem_language", problemLanguage.fk)

  /** Supports admission checks and oldest-first worker claims. */
  val byStatusAndCreatedAt = index("idx_problem_submissions_status_created_at", status, timestamps.createdAt)
}

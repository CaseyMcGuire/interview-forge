package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** One user's code snapshot and execution summary, shared by Run and Submit workflows. */
class Submission : EntSchema("submissions", clientName = "submissions") {
  // Database-generated identity for a single attempt; running again creates another submission.
  override fun id() = EntId.long()

  // Owner of this attempt; future writes must derive it from the authenticated user.
  val userId by long("user_id").immutable()

  // Existing user account; historical attempts must not disappear through a cascading user deletion.
  val user by belongsTo<User>("user").field(userId).onDelete(OnDelete.RESTRICT)

  // Problem used for history filtering; must match the selected problem-language configuration.
  val problemId by long("problem_id").immutable()

  // Stable problem identity; current problem text and tests may differ from when this attempt ran.
  val problem by belongsTo<Problem>("problem").field(problemId)
    .inverse(Problem::submissions).onDelete(OnDelete.RESTRICT)

  // Selected language configuration; its association is fixed even if its stencil is later edited.
  val problemLanguageId by long("problem_language_id").immutable()

  // Language and starter-code provenance; this is not a snapshot of the old harness or stencil.
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language").field(problemLanguageId)
    .inverse(ProblemLanguage::submissions).onDelete(OnDelete.RESTRICT)

  // Runtime configuration chosen when enqueueing; copy it instead of reading mutable judge settings later.
  val runtimeKey by string("runtime_key").immutable()

  // Exact submitted source; retain it independently of later editor or starter-code changes.
  val sourceCode by text("source_code").immutable().sensitive()

  // RUN uses examples/custom inputs; SUBMIT uses the official suite and can count as a solved problem.
  val kind by enum<SubmissionKind>("kind").immutable()

  // Execution lifecycle, initially queued; only a trusted grading path should advance it.
  val status by enum<SubmissionStatus>("status").default(SubmissionStatus.QUEUED)

  // Null until execution finishes; ACCEPTED is reserved for successful official submissions.
  val verdict by enum<SubmissionVerdict>("verdict").nullable()

  // Number of snapshotted cases selected before enqueueing; must match the result rows for this attempt.
  val totalCases by int("total_cases").immutable()

  // Number of cases with a PASSED outcome; grading must keep it between zero and totalCases.
  val passedCases by int("passed_cases").default(0)

  // Sum of measured case execution times in milliseconds; null before timing is available.
  val runtimeMs by long("runtime_ms").nullable()

  // Highest measured memory use among executed cases, in megabytes; null if unavailable.
  val peakMemoryMb by int("peak_memory_mb").nullable()

  // Safe user-facing failure explanation; exclude hidden inputs and private harness/checker diagnostics.
  val publicErrorMessage by text("public_error_message").nullable().sensitive()

  // Null while queued; set when execution begins.
  val startedAt by time("started_at").nullable()

  // Null until terminal completion, including compilation and infrastructure failures.
  val finishedAt by time("finished_at").nullable()

  // Per-case snapshots and outcomes; hidden-case details require stricter access than this summary.
  val testResults by hasMany<SubmissionTestResult>("test_results")

  // Time the user created this attempt, used to order submission history.
  val createdAt by time("created_at").defaultNow().immutable()

  // Time the lifecycle, verdict, or execution summary was last updated.
  val updatedAt by time("updated_at").defaultNow().updateDefaultNow()

  // Supports a user's complete submission history.
  val byUserAndCreatedAt = index("idx_submissions_user_created_at", userId, createdAt)

  // Supports a user's attempts and solved-state queries for one problem.
  val byUserProblemAndCreatedAt = index("idx_submissions_user_problem_created_at", userId, problemId, createdAt)

  // Supports problem-level history and the problem foreign key.
  val byProblem = index("idx_submissions_problem", problemId)

  // Supports language-configuration history and its foreign key.
  val byProblemLanguage = index("idx_submissions_problem_language", problemLanguageId)
}

package com.application.schema

/** Difficulty shown when browsing the shared problem catalog. */
enum class ProblemDifficulty {
  EASY,
  MEDIUM,
  HARD,
}

/** How official outputs are judged; custom checker source lives in JudgeConfiguration. */
enum class ProblemCheckerKind {
  EXACT_JSON,
  CUSTOM,
}

/** Whether an official test is a public example or private grading input. */
enum class TestCaseVisibility {
  EXAMPLE,
  HIDDEN,
}

/** Execution lifecycle, separate from whether the submitted code was correct. */
enum class SubmissionStatus {
  QUEUED,
  RUNNING,
  FINISHED,
}

/** Overall result; a finished execution can still have a failing verdict. */
enum class SubmissionVerdict {
  PENDING,
  ACCEPTED,
  WRONG_ANSWER,
  COMPILE_ERROR,
  RUNTIME_ERROR,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  INTERNAL_ERROR,
  /** A Run action completed; individual cases may still report comparison failures. */
  EXECUTED,
}

/** Test origin captured for history and access decisions, even if the original test changes. */
enum class SubmissionTestSource {
  EXAMPLE,
  HIDDEN,
  CUSTOM,
}

/** Per-case result; compile failures belong to the submission and leave cases skipped. */
enum class SubmissionTestOutcome {
  PENDING,
  PASSED,
  WRONG_ANSWER,
  RUNTIME_ERROR,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  INTERNAL_ERROR,
  /** A custom case returned successfully without an expected output to compare against. */
  EXECUTED,
  SKIPPED,
}

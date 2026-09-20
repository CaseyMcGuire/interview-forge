package com.application.schema

/** Preparation, compilation, and case execution share one RUNNING lifecycle state. */
enum class CustomInputSubmissionStatus {
  QUEUED,
  RUNNING,
  FINISHED,
}

/** Overall custom-run result, including failures that occur before user cases execute. */
enum class CustomInputSubmissionOutcome {
  PASSED,
  WRONG_ANSWER,
  INVALID_OUTPUT,
  COMPILE_ERROR,
  RUNTIME_ERROR,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  OUTPUT_LIMIT_EXCEEDED,
  REFERENCE_SOLUTION_FAILED,
  INTERNAL_ERROR,
}

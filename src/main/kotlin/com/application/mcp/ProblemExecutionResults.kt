package com.application.mcp

import com.application.schema.ProblemSubmissionStatus
import com.application.schema.ProblemSubmissionVerdict

data class ExpectedOutputResult(val expectedOutputJson: String)

data class ProblemSubmissionLookupResult(val problemSubmission: ProblemSubmissionDetails?)

data class ProblemSubmissionDetails(
  val id: String,
  val status: ProblemSubmissionStatus,
  val verdict: ProblemSubmissionVerdict,
  val totalCases: Int,
  val passedCases: Int,
  val runtimeMs: Long?,
  val publicErrorMessage: String?,
  val createdAt: String,
  val startedAt: String?,
  val finishedAt: String?,
  val failedExample: FailedExampleDetails? = null,
)

data class FailedExampleDetails(
  val inputJson: String,
  val expectedOutputJson: String,
  val output: String,
)

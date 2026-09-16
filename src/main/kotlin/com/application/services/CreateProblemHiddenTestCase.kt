package com.application.services

data class CreateProblemHiddenTestCase(
  val problemId: Long,
  val inputJson: String,
  val expectedOutputJson: String,
  val explanationMarkdown: String? = null,
)

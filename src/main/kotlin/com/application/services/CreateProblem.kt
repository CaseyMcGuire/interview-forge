package com.application.services

import com.application.schema.ProblemDifficulty

data class CreateProblem(
  val slug: String,
  val title: String,
  val statementMarkdown: String,
  val difficulty: ProblemDifficulty,
  val languageConfigurations: List<CreateProblemLanguage>,
  val examples: List<CreateProblemTestCase>,
  val testCases: List<CreateProblemTestCase> = emptyList(),
  val tagIds: List<Long> = emptyList(),
)

data class CreateProblemLanguage(
  val languageKey: String,
  val starterCode: String,
  val judgeConfiguration: ProblemJudgeConfiguration? = null,
)

data class ProblemJudgeConfiguration(
  val testDriverCode: String,
  val referenceSolutionCode: String?,
  val timeLimitMs: Int,
  val memoryLimitMb: Int,
)

data class CreateProblemTestCase(
  val inputJson: String,
  val expectedOutputJson: String,
  val explanationMarkdown: String?,
)

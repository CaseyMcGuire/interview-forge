package com.application.services

import com.application.schema.ProblemDifficulty

data class CreateProblem(
  val slug: String,
  val title: String,
  val statementMarkdown: String,
  val difficulty: ProblemDifficulty,
  val languageConfigurations: List<CreateProblemLanguage>,
  val examples: List<CreateProblemExample>,
)

data class CreateProblemLanguage(
  val languageKey: String,
  val starterCode: String,
)

data class CreateProblemExample(
  val inputJson: String,
  val expectedOutputJson: String,
  val explanationMarkdown: String?,
)

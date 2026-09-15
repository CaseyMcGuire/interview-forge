package com.application.services

import com.application.schema.ProblemDifficulty

data class UpdateProblem(
  val id: Long,
  val title: String? = null,
  val statementMarkdown: String? = null,
  val difficulty: ProblemDifficulty? = null,
)

data class UpdateProblemLanguage(
  val id: Long,
  val starterCode: String? = null,
)

data class UpdateProblemExample(
  val id: Long,
  val inputJson: String? = null,
  val expectedOutputJson: String? = null,
  val explanationMarkdown: FieldUpdate<String?> = FieldUpdate.Unchanged,
)

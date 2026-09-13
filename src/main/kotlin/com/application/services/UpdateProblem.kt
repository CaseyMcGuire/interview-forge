package com.application.services

import com.application.schema.ProblemDifficulty

/**
 * Service input with decoded database IDs and the schema's domain enum.
 * The GraphQL boundary converts its generated input here, just as it does for CreateProblem,
 * so the service does not need to understand GraphQL global IDs or generated API types.
 */
data class UpdateProblem(
  val title: String,
  val statementMarkdown: String,
  val difficulty: ProblemDifficulty,
  val languageConfigurations: List<UpdateProblemLanguage>,
  val examples: List<UpdateProblemExample>,
)

data class UpdateProblemLanguage(
  val id: Long,
  val starterCode: String,
  val solutionFilename: String,
)

data class UpdateProblemExample(
  val id: Long,
  val inputJson: String,
  val expectedOutputJson: String,
  val explanationMarkdown: String?,
)

class ProblemContentChangedException(message: String) : RuntimeException(message)

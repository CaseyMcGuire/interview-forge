package com.application.mcp

import com.application.ent.TestCase
import com.application.schema.ProblemDifficulty

data class LanguageCatalogResult(val languages: List<LanguageSummary>)

data class LanguageSummary(val key: String, val displayName: String)

data class ProblemSearchResult(
  val problems: List<ProblemSummary>,
  val nextCursor: String?,
)

data class ProblemSummary(
  val slug: String,
  val title: String,
  val difficulty: ProblemDifficulty,
)

data class ProblemLookupResult(val problem: ProblemDetails?)

data class ProblemDetails(
  val slug: String,
  val title: String,
  val statementMarkdown: String,
  val difficulty: ProblemDifficulty,
  val languageConfigurations: List<ProblemLanguageDetails>,
  val publicExamples: List<TestCaseDetails>,
  val testCases: List<TestCaseDetails>,
)

data class ProblemLanguageDetails(
  val languageKey: String,
  val starterCode: String,
  val judgeConfiguration: JudgeConfigurationDetails?,
)

data class JudgeConfigurationDetails(
  val testDriverCode: String,
  val referenceSolutionCode: String?,
  val checkerSource: String?,
  val timeLimitMs: Int,
  val memoryLimitMb: Int,
)

data class TestCaseDetails(
  val id: String,
  val position: Int,
  val inputJson: String,
  val expectedOutputJson: String,
  val explanationMarkdown: String?,
)

internal fun TestCase.toTestCaseDetails() = TestCaseDetails(
  id = id.toString(),
  position = position,
  inputJson = inputJson.toString(),
  expectedOutputJson = expectedOutputJson.toString(),
  explanationMarkdown = explanationMarkdown,
)

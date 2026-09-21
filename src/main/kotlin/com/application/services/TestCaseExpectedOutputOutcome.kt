package com.application.services

import kotlinx.serialization.json.JsonElement

sealed interface TestCaseExpectedOutputOutcome {
  data class Success(val expectedOutput: JsonElement) : TestCaseExpectedOutputOutcome
  data object NotFound : TestCaseExpectedOutputOutcome
  data object Unavailable : TestCaseExpectedOutputOutcome
  data class ReferenceFailed(val message: String) : TestCaseExpectedOutputOutcome
}

package com.application.services

sealed interface EnqueueCustomTestSuiteRunOutcome {
  class Success(val customTestSuiteRunId: Long) : EnqueueCustomTestSuiteRunOutcome

  data object AuthenticationRequired : EnqueueCustomTestSuiteRunOutcome
  data object NotFound : EnqueueCustomTestSuiteRunOutcome
  data object Unavailable : EnqueueCustomTestSuiteRunOutcome
  data object Busy : EnqueueCustomTestSuiteRunOutcome
}

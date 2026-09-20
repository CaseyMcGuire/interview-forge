package com.application.services

sealed interface EnqueueCustomInputSubmissionOutcome {
  class Success(val customInputSubmissionId: Long) : EnqueueCustomInputSubmissionOutcome

  data object AuthenticationRequired : EnqueueCustomInputSubmissionOutcome
  data object NotFound : EnqueueCustomInputSubmissionOutcome
  data object Unavailable : EnqueueCustomInputSubmissionOutcome
  data object Busy : EnqueueCustomInputSubmissionOutcome
}

package com.application.services

import com.application.ent.Submission

sealed interface SubmitSolutionOutcome {
  class Success(val submission: Submission) : SubmitSolutionOutcome

  data object AuthenticationRequired : SubmitSolutionOutcome
  data object NotFound : SubmitSolutionOutcome
  data object Unavailable : SubmitSolutionOutcome
  data object Busy : SubmitSolutionOutcome
}

package com.application.services

import com.application.ent.ProblemSubmission

sealed interface SubmitSolutionOutcome {
  class Success(val problemSubmission: ProblemSubmission) : SubmitSolutionOutcome

  data object AuthenticationRequired : SubmitSolutionOutcome
  data object NotFound : SubmitSolutionOutcome
  data object Unavailable : SubmitSolutionOutcome
  data object Busy : SubmitSolutionOutcome
}

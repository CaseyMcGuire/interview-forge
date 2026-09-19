package com.application.graphql

import com.application.graphql.types.AuthenticationRequired
import com.application.graphql.types.ExecutionBusy
import com.application.graphql.types.ExecutionUnavailable
import com.application.graphql.types.FieldError
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemNotFound
import com.application.graphql.types.SubmitSolutionInput
import com.application.graphql.types.SubmitSolutionResult
import com.application.graphql.types.SubmitSolutionSuccess
import com.application.graphql.types.Submission
import com.application.graphql.types.SubmissionFailedExample
import com.application.graphql.types.SubmissionStatus
import com.application.graphql.types.SubmissionValidationFailure
import com.application.graphql.types.SubmissionVerdict
import com.application.services.SubmitSolutionOutcome
import com.application.services.SubmissionService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import entkt.runtime.result.EntValidationException
import com.application.ent.Submission as SubmissionEntity

@DgsComponent
class SubmissionDataFetcher(
  private val submissionService: SubmissionService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsMutation
  fun submitSolution(@InputArgument input: SubmitSolutionInput): SubmitSolutionResult {
    val result = try {
      submissionService.submitSolution(
        problemLanguageId = globalIdUtil.fromGlobalIdOrNull(input.problemLanguageId, ProblemLanguage::class),
        sourceCode = input.sourceCode,
      )
    } catch (exception: EntValidationException) {
      return SubmissionValidationFailure(
        message = "Invalid submission input",
        fieldErrors = exception.violations.map {
          FieldError(field = it.field.orEmpty(), message = it.message)
        },
      )
    }

    return when (result) {
      is SubmitSolutionOutcome.Success -> SubmitSolutionSuccess(toGraphqlSubmission(result.submission))

      SubmitSolutionOutcome.AuthenticationRequired -> AuthenticationRequired("Sign in to submit a solution")
      SubmitSolutionOutcome.NotFound -> ProblemNotFound("Problem language is unavailable")
      SubmitSolutionOutcome.Unavailable -> ExecutionUnavailable("Execution is unavailable for this problem and language")
      SubmitSolutionOutcome.Busy -> ExecutionBusy("Execution capacity is full; try again later")
    }
  }

  @DgsQuery
  fun submission(@InputArgument id: String): Submission? {
    val databaseId = globalIdUtil.fromGlobalIdOrNull(id, Submission::class) ?: return null

    return submissionService.findSubmissionForCurrentUser(databaseId)?.let(::toGraphqlSubmission)
  }

  @DgsData(
    parentType = DgsConstants.SUBMISSION.TYPE_NAME,
    field = DgsConstants.SUBMISSION.FailedExample,
  )
  fun failedExample(environment: DgsDataFetchingEnvironment): SubmissionFailedExample? {
    val submission = environment.getSource<Submission>() ?: return null
    val id = globalIdUtil.fromGlobalIdOrNull(submission.id, Submission::class) ?: return null
    val failedCase = submissionService.findFailedExampleForCurrentUser(id) ?: return null

    return SubmissionFailedExample(
      inputJson = failedCase.inputJson.toString(),
      expectedOutputJson = checkNotNull(failedCase.expectedOutputJson).toString(),
      output = failedCase.stdout.orEmpty(),
    )
  }

  private fun toGraphqlSubmission(submission: SubmissionEntity): Submission = Submission(
    id = globalIdUtil.toGlobalId(Submission::class, submission.id),
    status = SubmissionStatus.valueOf(submission.status.name),
    verdict = SubmissionVerdict.valueOf(submission.verdict.name),
    totalCases = submission.totalCases,
    passedCases = submission.passedCases,
    runtimeMs = submission.runtimeMs?.let(Math::toIntExact),
    peakMemoryMb = submission.peakMemoryMb,
    publicErrorMessage = submission.publicErrorMessage?.take(20_000),
    createdAt = submission.createdAt.toString(),
    startedAt = submission.startedAt?.toString(),
    finishedAt = submission.finishedAt?.toString(),
  )
}

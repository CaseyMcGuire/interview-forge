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
import com.application.graphql.types.ProblemSubmission
import com.application.graphql.types.ProblemSubmissionFailedExample
import com.application.graphql.types.ProblemSubmissionStatus
import com.application.graphql.types.ProblemSubmissionValidationFailure
import com.application.graphql.types.ProblemSubmissionVerdict
import com.application.services.SubmitSolutionOutcome
import com.application.services.ProblemSubmissionService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import entkt.runtime.result.EntValidationException
import com.application.ent.ProblemSubmission as ProblemSubmissionEntity

@DgsComponent
class ProblemSubmissionDataFetcher(
  private val problemSubmissionService: ProblemSubmissionService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsMutation
  fun submitSolution(@InputArgument input: SubmitSolutionInput): SubmitSolutionResult {
    val result = try {
      problemSubmissionService.submitSolution(
        problemLanguageId = globalIdUtil.fromGlobalIdOrNull(input.problemLanguageId, ProblemLanguage::class),
        sourceCode = input.sourceCode,
      )
    } catch (exception: EntValidationException) {
      return ProblemSubmissionValidationFailure(
        message = "Invalid submission input",
        fieldErrors = exception.violations.map {
          FieldError(field = it.field.orEmpty(), message = it.message)
        },
      )
    }

    return when (result) {
      is SubmitSolutionOutcome.Success -> SubmitSolutionSuccess(toGraphqlProblemSubmission(result.problemSubmission))

      SubmitSolutionOutcome.AuthenticationRequired -> AuthenticationRequired("Sign in to submit a solution")
      SubmitSolutionOutcome.NotFound -> ProblemNotFound("Problem language is unavailable")
      SubmitSolutionOutcome.Unavailable -> ExecutionUnavailable("Execution is unavailable for this problem and language")
      SubmitSolutionOutcome.Busy -> ExecutionBusy("Execution capacity is full; try again later")
    }
  }

  @DgsQuery
  fun problemSubmission(@InputArgument id: String): ProblemSubmission? {
    val databaseId = globalIdUtil.fromGlobalIdOrNull(id, ProblemSubmission::class) ?: return null

    return problemSubmissionService.findProblemSubmissionForCurrentUser(databaseId)?.let(::toGraphqlProblemSubmission)
  }

  @DgsData(
    parentType = DgsConstants.PROBLEMSUBMISSION.TYPE_NAME,
    field = DgsConstants.PROBLEMSUBMISSION.FailedExample,
  )
  fun failedExample(environment: DgsDataFetchingEnvironment): ProblemSubmissionFailedExample? {
    val problemSubmission = environment.getSource<ProblemSubmission>() ?: return null
    val id = globalIdUtil.fromGlobalIdOrNull(problemSubmission.id, ProblemSubmission::class) ?: return null
    val failedCase = problemSubmissionService.findFailedExampleForCurrentUser(id) ?: return null

    return ProblemSubmissionFailedExample(
      inputJson = failedCase.inputJson.toString(),
      expectedOutputJson = checkNotNull(failedCase.expectedOutputJson).toString(),
      output = failedCase.stdout.orEmpty(),
    )
  }

  private fun toGraphqlProblemSubmission(problemSubmission: ProblemSubmissionEntity): ProblemSubmission = ProblemSubmission(
    id = globalIdUtil.toGlobalId(ProblemSubmission::class, problemSubmission.id),
    status = ProblemSubmissionStatus.valueOf(problemSubmission.status.name),
    verdict = ProblemSubmissionVerdict.valueOf(problemSubmission.verdict.name),
    totalCases = problemSubmission.totalCases,
    passedCases = problemSubmission.passedCases,
    runtimeMs = problemSubmission.runtimeMs?.let(Math::toIntExact),
    peakMemoryMb = problemSubmission.peakMemoryMb,
    publicErrorMessage = problemSubmission.publicErrorMessage?.take(20_000),
    createdAt = problemSubmission.createdAt.toString(),
    startedAt = problemSubmission.startedAt?.toString(),
    finishedAt = problemSubmission.finishedAt?.toString(),
  )
}

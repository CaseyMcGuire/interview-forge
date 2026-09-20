package com.application.graphql

import com.application.execution.CustomTestCaseResult
import com.application.execution.customTestCaseErrorMessage
import com.application.graphql.types.AuthenticationRequired
import com.application.graphql.types.CustomTestCase
import com.application.graphql.types.CustomInputSubmission
import com.application.graphql.types.CustomInputSubmissionCaseOutcome
import com.application.graphql.types.CustomInputSubmissionCaseResult
import com.application.graphql.types.CustomInputSubmissionOutcome
import com.application.graphql.types.CustomInputSubmissionStatus
import com.application.graphql.types.EnqueueCustomInputSubmissionInput
import com.application.graphql.types.EnqueueCustomInputSubmissionResult
import com.application.graphql.types.EnqueueCustomInputSubmissionSuccess
import com.application.graphql.types.EnqueueCustomInputSubmissionValidationFailure
import com.application.graphql.types.ExecutionBusy
import com.application.graphql.types.ExecutionUnavailable
import com.application.graphql.types.FieldError
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemNotFound
import com.application.services.CustomInputSubmissionService
import com.application.services.EnqueueCustomInputSubmissionInputException
import com.application.services.EnqueueCustomInputSubmissionOutcome
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntValidationException
import kotlinx.serialization.json.jsonObject
import com.application.ent.CustomTestCase as CustomTestCaseEntity
import com.application.ent.CustomInputSubmission as CustomInputSubmissionEntity
import com.application.schema.CustomInputSubmissionStatus as StoredCustomInputSubmissionStatus

@DgsComponent
class CustomInputSubmissionDataFetcher(
  private val service: CustomInputSubmissionService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsMutation
  fun enqueueCustomInputSubmission(
    @InputArgument input: EnqueueCustomInputSubmissionInput,
  ): EnqueueCustomInputSubmissionResult {
    val result = try {
      service.enqueueCustomInputSubmission(
        problemLanguageId = globalIdUtil.fromGlobalIdOrNull(input.problemLanguageId, ProblemLanguage::class),
        sourceCode = input.sourceCode,
        caseInputs = input.cases.map { it.inputJson },
      )
    } catch (exception: EnqueueCustomInputSubmissionInputException) {
      return EnqueueCustomInputSubmissionValidationFailure(
        message = "Invalid test inputs",
        fieldErrors = listOf(FieldError(field = exception.field, message = exception.message)),
      )
    } catch (exception: EntValidationException) {
      return EnqueueCustomInputSubmissionValidationFailure(
        message = "Invalid test inputs",
        fieldErrors = exception.violations.map {
          FieldError(field = it.field.orEmpty(), message = it.message)
        },
      )
    }

    return when (result) {
      is EnqueueCustomInputSubmissionOutcome.Success -> EnqueueCustomInputSubmissionSuccess(
        customInputSubmissionId = globalIdUtil.toGlobalId(CustomInputSubmission::class, result.customInputSubmissionId),
      )

      EnqueueCustomInputSubmissionOutcome.AuthenticationRequired -> AuthenticationRequired("Sign in to run tests")
      EnqueueCustomInputSubmissionOutcome.NotFound -> ProblemNotFound("Problem language is unavailable")
      EnqueueCustomInputSubmissionOutcome.Unavailable -> ExecutionUnavailable(
        "Custom execution is unavailable for this problem and language",
      )

      EnqueueCustomInputSubmissionOutcome.Busy -> ExecutionBusy("Execution capacity is full; try again later")
    }
  }

  @DgsQuery
  fun customInputSubmission(@InputArgument id: String): CustomInputSubmission? {
    val databaseId = globalIdUtil.fromGlobalIdOrNull(id, CustomInputSubmission::class) ?: return null
    val customInputSubmission = service.findCustomInputSubmissionForCurrentUser(databaseId) ?: return null

    return CustomInputSubmission(
      id = globalIdUtil.toGlobalId(CustomInputSubmission::class, customInputSubmission.id),
      status = CustomInputSubmissionStatus.valueOf(customInputSubmission.status.name),
      outcome = customInputSubmission.outcome?.let { CustomInputSubmissionOutcome.valueOf(it.name) },
      totalCases = customInputSubmission.totalCases,
      passedCases = customInputSubmission.passedCases,
      caseResults = toGraphqlCaseResults(customInputSubmission),
      runtimeMs = customInputSubmission.runtimeMs?.let(Math::toIntExact),
      publicErrorMessage = customInputSubmission.publicErrorMessage?.take(20_000),
      createdAt = customInputSubmission.createdAt.toString(),
      startedAt = customInputSubmission.startedAt?.toString(),
      finishedAt = customInputSubmission.finishedAt?.toString(),
    )
  }

  private fun toGraphqlCaseResults(customInputSubmission: CustomInputSubmissionEntity): List<CustomInputSubmissionCaseResult>? {
    if (customInputSubmission.status != StoredCustomInputSubmissionStatus.FINISHED) {
      return null
    }

    val results = customInputSubmission.caseResults ?: return null
    val cases = customInputSubmission.edges.cases.requireLoaded()
    val resultsByCaseId = results.associate { result ->
      val caseResult = CustomTestCaseResult.fromJson(result.jsonObject)
      caseResult.testCaseId to caseResult
    }

    check(resultsByCaseId.size == cases.size && results.size == cases.size) { "Incomplete custom case results" }

    return cases.map { testCase ->
      val result = resultsByCaseId.getValue(testCase.id)

      CustomInputSubmissionCaseResult(
        testCase = toGraphqlTestCase(testCase),
        outcome = CustomInputSubmissionCaseOutcome.valueOf(result.outcome.name),
        output = result.output.take(20_000),
        publicErrorMessage = customTestCaseErrorMessage(result.outcome),
      )
    }
  }

  private fun toGraphqlTestCase(testCase: CustomTestCaseEntity): CustomTestCase = CustomTestCase(
    id = globalIdUtil.toGlobalId(CustomTestCase::class, testCase.id),
    position = testCase.position,
    inputJson = testCase.inputJson.toString(),
    expectedOutputJson = testCase.expectedOutputJson?.toString(),
  )
}

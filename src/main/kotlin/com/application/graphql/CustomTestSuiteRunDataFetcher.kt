package com.application.graphql

import com.application.graphql.types.AuthenticationRequired
import com.application.graphql.types.CustomTestCase
import com.application.graphql.types.CustomTestSuiteRun
import com.application.graphql.types.CustomTestSuiteRunCaseOutcome
import com.application.graphql.types.CustomTestSuiteRunCaseResult
import com.application.graphql.types.CustomTestSuiteRunOutcome
import com.application.graphql.types.CustomTestSuiteRunStatus
import com.application.graphql.types.EnqueueCustomTestSuiteRunInput
import com.application.graphql.types.EnqueueCustomTestSuiteRunResult
import com.application.graphql.types.EnqueueCustomTestSuiteRunSuccess
import com.application.graphql.types.EnqueueCustomTestSuiteRunValidationFailure
import com.application.graphql.types.ExecutionBusy
import com.application.graphql.types.ExecutionUnavailable
import com.application.graphql.types.FieldError
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemNotFound
import com.application.services.CustomTestSuiteRunService
import com.application.services.EnqueueCustomTestSuiteRunInputException
import com.application.services.EnqueueCustomTestSuiteRunOutcome
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import entkt.runtime.query.requireLoaded
import entkt.runtime.result.EntValidationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import com.application.ent.CustomTestCase as CustomTestCaseEntity
import com.application.ent.CustomTestSuiteRun as CustomTestSuiteRunEntity
import com.application.schema.CustomTestSuiteRunStatus as StoredRunStatus

@DgsComponent
class CustomTestSuiteRunDataFetcher(
  private val service: CustomTestSuiteRunService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsMutation
  fun enqueueCustomTestSuiteRun(
    @InputArgument input: EnqueueCustomTestSuiteRunInput,
  ): EnqueueCustomTestSuiteRunResult {
    val result = try {
      service.enqueueCustomTestSuiteRun(
        problemLanguageId = globalIdUtil.fromGlobalIdOrNull(input.problemLanguageId, ProblemLanguage::class),
        sourceCode = input.sourceCode,
        caseInputs = input.cases.map { it.inputJson },
      )
    } catch (exception: EnqueueCustomTestSuiteRunInputException) {
      return EnqueueCustomTestSuiteRunValidationFailure(
        message = "Invalid test run input",
        fieldErrors = listOf(FieldError(field = exception.field, message = exception.message)),
      )
    } catch (exception: EntValidationException) {
      return EnqueueCustomTestSuiteRunValidationFailure(
        message = "Invalid test run input",
        fieldErrors = exception.violations.map {
          FieldError(field = it.field.orEmpty(), message = it.message)
        },
      )
    }

    return when (result) {
      is EnqueueCustomTestSuiteRunOutcome.Success -> EnqueueCustomTestSuiteRunSuccess(
        customTestSuiteRunId = globalIdUtil.toGlobalId(CustomTestSuiteRun::class, result.customTestSuiteRunId),
      )

      EnqueueCustomTestSuiteRunOutcome.AuthenticationRequired -> AuthenticationRequired("Sign in to run tests")
      EnqueueCustomTestSuiteRunOutcome.NotFound -> ProblemNotFound("Problem language is unavailable")
      EnqueueCustomTestSuiteRunOutcome.Unavailable -> ExecutionUnavailable(
        "Custom execution is unavailable for this problem and language",
      )

      EnqueueCustomTestSuiteRunOutcome.Busy -> ExecutionBusy("Execution capacity is full; try again later")
    }
  }

  @DgsQuery
  fun customTestSuiteRun(@InputArgument id: String): CustomTestSuiteRun? {
    val databaseId = globalIdUtil.fromGlobalIdOrNull(id, CustomTestSuiteRun::class) ?: return null
    val run = service.findRunForCurrentUser(databaseId) ?: return null

    return CustomTestSuiteRun(
      id = globalIdUtil.toGlobalId(CustomTestSuiteRun::class, run.id),
      status = CustomTestSuiteRunStatus.valueOf(run.status.name),
      outcome = run.outcome?.let { CustomTestSuiteRunOutcome.valueOf(it.name) },
      totalCases = run.totalCases,
      passedCases = run.passedCases,
      caseResults = toGraphqlCaseResults(run),
      runtimeMs = run.runtimeMs?.let(Math::toIntExact),
      publicErrorMessage = run.publicErrorMessage?.take(20_000),
      createdAt = run.createdAt.toString(),
      startedAt = run.startedAt?.toString(),
      finishedAt = run.finishedAt?.toString(),
    )
  }

  private fun toGraphqlCaseResults(run: CustomTestSuiteRunEntity): List<CustomTestSuiteRunCaseResult>? {
    if (run.status != StoredRunStatus.FINISHED) {
      return null
    }

    val results = run.caseResults ?: return null
    val cases = run.edges.cases.requireLoaded()
    val resultsByCaseId = results.associate { result ->
      val fields = result.jsonObject
      fields.getValue("testCaseId").jsonPrimitive.long to fields
    }

    check(resultsByCaseId.size == cases.size && results.size == cases.size) { "Incomplete custom case results" }

    return cases.map { testCase ->
      val result = resultsByCaseId.getValue(testCase.id)

      CustomTestSuiteRunCaseResult(
        testCase = toGraphqlTestCase(testCase),
        outcome = CustomTestSuiteRunCaseOutcome.valueOf(result.getValue("outcome").jsonPrimitive.content),
        output = result.getValue("output").jsonPrimitive.content.take(20_000),
        publicErrorMessage = result["publicErrorMessage"]?.jsonPrimitive?.contentOrNull?.take(20_000),
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

package com.application.graphql

import com.application.graphql.types.ExecutionUnavailable
import com.application.graphql.types.FieldError
import com.application.graphql.types.GenerateTestCaseExpectedOutputInput
import com.application.graphql.types.GenerateTestCaseExpectedOutputResult
import com.application.graphql.types.GenerateTestCaseExpectedOutputSuccess
import com.application.graphql.types.ProblemForbidden
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemNotFound
import com.application.graphql.types.ProblemValidationFailure
import com.application.graphql.types.ReferenceSolutionFailed
import com.application.services.ProblemInputException
import com.application.services.TestCaseExpectedOutputOutcome
import com.application.services.TestCaseExpectedOutputService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import org.springframework.security.access.AccessDeniedException

@DgsComponent
class TestCaseExpectedOutputDataFetcher(
  private val service: TestCaseExpectedOutputService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsMutation
  fun generateTestCaseExpectedOutput(
    @InputArgument input: GenerateTestCaseExpectedOutputInput,
  ): GenerateTestCaseExpectedOutputResult {
    val result = try {
      service.generateExpectedOutput(
        problemLanguageId = globalIdUtil.fromGlobalIdOrNull(input.problemLanguageId, ProblemLanguage::class),
        inputJson = input.inputJson,
      )
    } catch (_: AccessDeniedException) {
      return ProblemForbidden("Administrator access is required")
    } catch (exception: ProblemInputException) {
      return ProblemValidationFailure(
        message = "Invalid test input",
        fieldErrors = listOf(FieldError(field = exception.field, message = exception.message)),
      )
    }

    return when (result) {
      is TestCaseExpectedOutputOutcome.Success -> GenerateTestCaseExpectedOutputSuccess(result.expectedOutput.toString())
      TestCaseExpectedOutputOutcome.NotFound -> ProblemNotFound("Problem language is unavailable")
      TestCaseExpectedOutputOutcome.Unavailable -> ExecutionUnavailable(
        "Reference execution is unavailable. Check the reference solution and language runtime configuration.",
      )

      is TestCaseExpectedOutputOutcome.ReferenceFailed -> ReferenceSolutionFailed(result.message)
    }
  }
}

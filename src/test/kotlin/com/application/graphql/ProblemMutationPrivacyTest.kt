package com.application.graphql

import com.application.graphql.types.DeleteProblemExampleInput
import com.application.graphql.types.CreateJudgeConfigurationInput
import com.application.graphql.types.CreateProblemHiddenTestCaseInput
import com.application.graphql.types.JudgeConfiguration
import com.application.graphql.types.Problem
import com.application.graphql.types.ProblemExample
import com.application.graphql.types.ProblemLanguage
import com.application.graphql.types.ProblemNotFound
import com.application.graphql.types.ProblemTestCase
import com.application.graphql.types.UpdateJudgeConfigurationInput
import com.application.graphql.types.UpdateProblemExampleInput
import com.application.graphql.types.UpdateProblemInput
import com.application.graphql.types.UpdateProblemLanguageInput
import com.application.graphql.types.UpdateProblemTestCaseInput
import com.application.services.ProblemService
import com.application.services.CreateProblemHiddenTestCase
import com.application.services.UpdateProblem
import com.application.services.UpdateProblemExample
import com.application.services.UpdateProblemLanguage
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntOperation
import entkt.runtime.result.MutationWriteState
import graphql.schema.DataFetchingEnvironmentImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ProblemMutationPrivacyTest {
  @Test
  fun `mutation privacy denials return not found without exposing the denial reason`() {
    val service = mock(ProblemService::class.java)
    val globalIdUtil = GlobalIdUtil()
    val fetcher = ProblemDataFetcher(service, globalIdUtil)
    val environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
      .arguments(mapOf("input" to emptyMap<String, Any?>()))
      .build()

    `when`(service.updateProblem(UpdateProblem(id = 42)))
      .thenThrow(denied("Problem", EntOperation.UPDATE))
    `when`(service.updateProblemLanguage(UpdateProblemLanguage(id = 42)))
      .thenThrow(denied("ProblemLanguage", EntOperation.UPDATE))
    `when`(service.updateProblemExample(UpdateProblemExample(id = 42)))
      .thenThrow(denied("TestCase", EntOperation.UPDATE))
    `when`(service.updateProblemTestCase(42, "[]", "[]", null))
      .thenThrow(denied("TestCase", EntOperation.UPDATE))
    `when`(service.deleteProblemExample(42))
      .thenThrow(denied("TestCase", EntOperation.DELETE))
    `when`(service.createProblemHiddenTestCase(CreateProblemHiddenTestCase(42, "[]", "[]")))
      .thenThrow(denied("TestCase", EntOperation.CREATE))
    `when`(service.createJudgeConfiguration(42, "driver", null, 1000, 256))
      .thenThrow(denied("JudgeConfiguration", EntOperation.CREATE))
    `when`(service.updateJudgeConfiguration(42))
      .thenThrow(denied("JudgeConfiguration", EntOperation.UPDATE))

    val expected = ProblemNotFound("The requested content does not exist or is unavailable")

    assertEquals(expected, fetcher.updateProblem(
      UpdateProblemInput(id = globalIdUtil.toGlobalId(Problem::class, 42)),
    ))
    assertEquals(expected, fetcher.updateProblemLanguage(
      UpdateProblemLanguageInput(id = globalIdUtil.toGlobalId(ProblemLanguage::class, 42)),
    ))
    assertEquals(expected, fetcher.updateProblemExample(
      UpdateProblemExampleInput(id = globalIdUtil.toGlobalId(ProblemExample::class, 42)),
      environment,
    ))
    assertEquals(expected, fetcher.updateProblemTestCase(
      UpdateProblemTestCaseInput(
        id = globalIdUtil.toGlobalId(ProblemTestCase::class, 42),
        inputJson = "[]",
        expectedOutputJson = "[]",
      ),
    ))
    assertEquals(expected, fetcher.deleteProblemExample(
      DeleteProblemExampleInput(id = globalIdUtil.toGlobalId(ProblemExample::class, 42)),
    ))
    assertEquals(expected, fetcher.createProblemHiddenTestCase(
      CreateProblemHiddenTestCaseInput(
        problemId = globalIdUtil.toGlobalId(Problem::class, 42),
        inputJson = "[]",
        expectedOutputJson = "[]",
      ),
    ))
    assertEquals(expected, fetcher.createJudgeConfiguration(judgeCreationInput(globalIdUtil)))
    assertEquals(expected, fetcher.updateJudgeConfiguration(
      UpdateJudgeConfigurationInput(id = globalIdUtil.toGlobalId(JudgeConfiguration::class, 42)),
      environment,
    ))
  }

  @Test
  fun `unexpected service failures propagate instead of becoming expected problem errors`() {
    val service = mock(ProblemService::class.java)
    val globalIdUtil = GlobalIdUtil()
    val fetcher = ProblemDataFetcher(service, globalIdUtil)
    val failure = IllegalStateException("Unexpected service failure")
    `when`(service.updateProblem(UpdateProblem(id = 42))).thenThrow(failure)
    `when`(service.createProblemHiddenTestCase(CreateProblemHiddenTestCase(42, "[]", "[]"))).thenThrow(failure)
    `when`(service.updateProblemTestCase(42, "[]", "[]", null)).thenThrow(failure)

    val thrown = assertThrows(IllegalStateException::class.java) {
      fetcher.updateProblem(UpdateProblemInput(id = globalIdUtil.toGlobalId(Problem::class, 42)))
    }

    assertSame(failure, thrown)

    val creationFailure = assertThrows(IllegalStateException::class.java) {
      fetcher.createProblemHiddenTestCase(CreateProblemHiddenTestCaseInput(
        problemId = globalIdUtil.toGlobalId(Problem::class, 42),
        inputJson = "[]",
        expectedOutputJson = "[]",
      ))
    }
    assertSame(failure, creationFailure)

    val testCaseFailure = assertThrows(IllegalStateException::class.java) {
      fetcher.updateProblemTestCase(UpdateProblemTestCaseInput(
        id = globalIdUtil.toGlobalId(ProblemTestCase::class, 42),
        inputJson = "[]",
        expectedOutputJson = "[]",
      ))
    }
    assertSame(failure, testCaseFailure)
  }

  private fun judgeCreationInput(globalIdUtil: GlobalIdUtil) = CreateJudgeConfigurationInput(
    problemLanguageId = globalIdUtil.toGlobalId(ProblemLanguage::class, 42),
    testDriverCode = "driver",
    timeLimitMs = 1000,
    memoryLimitMb = 256,
  )

  private fun denied(entityType: String, operation: EntOperation) = EntMutationPrivacyDeniedException(
    writeState = MutationWriteState.NotPersisted,
    entityType = entityType,
    operation = operation,
    entityKey = null,
    reason = "This record exists but is restricted to another viewer",
  )
}

package com.application.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinLanguageExecutionConfigTest {
  private val executionConfig: LanguageExecutionConfig = KotlinLanguageExecutionConfig()

  @Test
  fun `keeps solution and driver separate and preserves their imports and contents`() {
    val solution = """
      import java.util.ArrayDeque

      class Solution {
        fun solve(): Int = ArrayDeque(listOf(42)).first()
      }
    """.trimIndent() + "\n"
    val driver = """
      import java.io.BufferedReader

      fun main() {
        val input: BufferedReader = System.`in`.bufferedReader()
        input.readLine()
        println(Solution().solve())
      }
    """.trimIndent() + "\n"

    val program = executionConfig.prepare(solution, driver)

    assertEquals(mapOf("Solution.kt" to solution, "TestDriver.kt" to driver), program.sourceFiles)
    assertEquals(
      listOf("kotlinc", "Solution.kt", "TestDriver.kt", "-include-runtime", "-d", "submission.jar"),
      program.compileCommand,
    )
  }

  @Test
  fun `source content cannot change commands or select the solution main as the entry point`() {
    val driver = "fun main() = println(Solution().solve())"
    val ordinary = executionConfig.prepare("class Solution { fun solve() = 42 }", driver)
    val withMain = executionConfig.prepare(
      """
        class Solution { fun solve() = 42 }
        fun main() = println("not the test driver; && echo unexpected")
      """.trimIndent(),
      driver,
    )

    assertEquals(ordinary.compileCommand, withMain.compileCommand)
    assertEquals(ordinary.runCommand, withMain.runCommand)
    assertEquals(listOf("java", "-cp", "submission.jar", "TestDriverKt"), withMain.runCommand)
  }
}

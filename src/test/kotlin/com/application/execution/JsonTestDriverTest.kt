package com.application.execution

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class JsonTestDriverTest {
  data class Task(val id: Long, val labels: List<String>)
  data class TaskInput(val groups: Map<String, List<Task>>)
  data class NullableInput(val value: Int?)

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    JsonSubTypes.Type(value = Operation.Add::class, name = "add"),
    JsonSubTypes.Type(value = Operation.Count::class, name = "count"),
  )
  sealed interface Operation {
    data class Add(val value: Long) : Operation
    data class Count(val values: List<String>) : Operation
  }

  data class Input(val operations: List<Operation>)

  sealed interface Result {
    data class Added(val value: Long) : Result
    data class Counted(val count: Int) : Result
  }

  @Test
  fun `generic input properties retain their element types and integer precision`() {
    val driver = object : JsonTestDriver<TaskInput, List<Long>>(TaskInput::class) {
      override fun run(input: TaskInput): List<Long> {
        val task = input.groups.getValue("tasks").single()
        assertEquals(listOf("héllo 世界", "quote\""), task.labels)
        return listOf(task.id)
      }
    }

    val result = runDriver(driver, """{"groups":{"tasks":[{"id":9007199254740993,"labels":["héllo 世界","quote\""]}]}}""")

    assertEquals(Json.parseToJsonElement("[9007199254740993]"), result)
  }

  @Test
  fun `sealed operations and results keep problem logic typed`() {
    val driver = object : JsonTestDriver<Input, List<Result>>(Input::class) {
      override fun run(input: Input): List<Result> = input.operations.map { operation ->
        when (operation) {
          is Operation.Add -> Result.Added(operation.value + 1)
          is Operation.Count -> Result.Counted(operation.values.size)
        }
      }
    }

    val result = runDriver(driver, """{"operations":[{"type":"add","value":41},{"type":"count","values":["a","b"]}]}""")

    assertEquals(Json.parseToJsonElement("""[{"value":42},{"count":2}]"""), result)
  }

  @Test
  fun `nullable input properties and outputs support JSON null`() {
    val driver = object : JsonTestDriver<NullableInput, Int?>(NullableInput::class) {
      override fun run(input: NullableInput): Int? = input.value
    }

    assertEquals(JsonNull, runDriver(driver, """{"value":null}"""))
    assertEquals(JsonPrimitive(42), runDriver(driver, """{"value":42}"""))
  }

  @Test
  fun `JSON null cannot reach a nonnullable input even if run ignores it`() {
    var called = false
    val driver = object : JsonTestDriver<Int, Int>(Int::class) {
      override fun run(input: Int): Int {
        called = true
        return 42
      }
    }

    assertThrows(IllegalArgumentException::class.java) { runDriver(driver, "null") }
    assertFalse(called)
  }

  @Test
  fun `JSON null cannot reach a data class input`() {
    var called = false
    val driver = object : JsonTestDriver<Task, Int>(Task::class) {
      override fun run(input: Task): Int {
        called = true
        return 42
      }
    }

    assertThrows(IllegalArgumentException::class.java) { runDriver(driver, "null") }
    assertFalse(called)
  }

  @ParameterizedTest
  @ValueSource(strings = [
    """{"id":1.5,"labels":[]}""",
    """{"id":"1","labels":[]}""",
    """{"id":null,"labels":[]}""",
    """{"labels":[]}""",
    """{"id":1,"labels":[null]}""",
    """{"id":1,"labels":[],"typo":true}""",
    """{"id":1,"labels":[]} {}""",
  ])
  fun `invalid input types and shapes fail before calling the solution`(input: String) {
    var called = false
    val driver = object : JsonTestDriver<Task, Task>(Task::class) {
      override fun run(input: Task): Task {
        called = true
        return input
      }
    }

    assertThrows(RuntimeException::class.java) { runDriver(driver, input) }
    assertFalse(called)
  }

  @Test
  fun `unknown operation types fail before calling the solution`() {
    var called = false
    val driver = object : JsonTestDriver<Input, Int>(Input::class) {
      override fun run(input: Input): Int {
        called = true
        return 42
      }
    }

    assertThrows(RuntimeException::class.java) {
      runDriver(driver, """{"operations":[{"type":"unknown"}]}""")
    }
    assertFalse(called)
  }

  private fun runDriver(driver: JsonTestDriver<*, *>, input: String): JsonElement {
    val output = ByteArrayOutputStream()
    driver.writeJsonResult(input, output)
    return Json.parseToJsonElement(output.toString(Charsets.UTF_8))
  }
}

package com.application.execution

import kotlinx.serialization.json.JsonElement

/** One ordered input and the answer used by the grader. Expected answers stay outside execution. */
class TestCaseInput(val input: JsonElement, val expectedOutput: JsonElement)

package com.application.execution

import java.io.OutputStream
import kotlin.reflect.KClass
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Converts each JSON case into [I], calls [run], and serializes its result.
 * Define a public, default-package TestDriver subclass with a public no-argument constructor.
 * Pass a concrete input class such as Input::class; wrap generic or nullable inputs in its properties.
 *
 * The submitted source and driver compile together. Construct and call the submitted classes in [run];
 * the problem's starter code must declare the API those classes are expected to implement.
 *
 * The suite reuses that instance across cases; construction and JSON conversion count toward case limits.
 */
abstract class JsonTestDriver<I : Any, O>(private val inputClass: KClass<I>) {
  /** Runs one test case. Return the answer; use stderr for diagnostics. */
  abstract fun run(input: I): O

  /** Runtime entry point; streams the answer so the suite can enforce its output byte limit. */
  fun writeJsonResult(inputJson: String, output: OutputStream) {
    val input = requireNotNull(mapper.readValue(inputJson, inputClass.javaObjectType)) {
      "JSON null is not allowed for the input object"
    }

    val result = run(input)
    mapper.writeValue(output, result)
  }

  private companion object {
    val mapper = jsonMapper {
      addModule(kotlinModule())
      enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
      enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
      disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
    }
  }
}

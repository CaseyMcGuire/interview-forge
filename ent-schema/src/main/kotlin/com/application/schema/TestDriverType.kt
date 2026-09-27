package com.application.schema

/** Entry-point contract implemented by a problem's test driver. */
enum class TestDriverType {
  /** The driver reads JSON from stdin and writes its answer to stdout. */
  MAIN_FUNCTION,

  /** The runtime converts JSON to a typed input and serializes the driver's returned answer. */
  JSON_TEST_DRIVER,
}

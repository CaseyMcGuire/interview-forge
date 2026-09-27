package com.application.graphql

import com.application.graphql.types.TestDriverType
import com.application.schema.TestDriverType as SchemaTestDriverType

internal fun SchemaTestDriverType.toGraphqlType(): TestDriverType = when (this) {
  SchemaTestDriverType.MAIN_FUNCTION -> TestDriverType.MAIN_FUNCTION
  SchemaTestDriverType.JSON_TEST_DRIVER -> TestDriverType.JSON_TEST_DRIVER
}

internal fun TestDriverType.toSchemaType(): SchemaTestDriverType = when (this) {
  TestDriverType.MAIN_FUNCTION -> SchemaTestDriverType.MAIN_FUNCTION
  TestDriverType.JSON_TEST_DRIVER -> SchemaTestDriverType.JSON_TEST_DRIVER
}

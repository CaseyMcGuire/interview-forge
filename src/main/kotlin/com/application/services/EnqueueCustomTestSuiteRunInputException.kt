package com.application.services

/** Invalid request structure or a case error identified by its position in the submitted list. */
class EnqueueCustomTestSuiteRunInputException(
  val field: String,
  override val message: String,
) : IllegalArgumentException(message)

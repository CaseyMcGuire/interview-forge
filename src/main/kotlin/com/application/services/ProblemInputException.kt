package com.application.services

/** An input value could not be decoded into a domain value, such as an ID or JSON document. */
class ProblemInputException(
  val field: String,
  override val message: String,
) : IllegalArgumentException(message)

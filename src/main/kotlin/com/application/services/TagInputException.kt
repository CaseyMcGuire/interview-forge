package com.application.services

class TagInputException(
  val field: String,
  override val message: String,
) : IllegalArgumentException(message)

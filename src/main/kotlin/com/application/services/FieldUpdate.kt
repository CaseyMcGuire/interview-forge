package com.application.services

/** A partial update; Set(null) explicitly clears a nullable field. */
sealed interface FieldUpdate<out T> {
  data object Unchanged : FieldUpdate<Nothing>

  data class Set<T>(val value: T) : FieldUpdate<T>
}

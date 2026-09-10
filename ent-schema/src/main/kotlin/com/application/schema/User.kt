package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema

// Maps the existing V1 Flyway migration; the database retains its VARCHAR(255) constraints.
class User : EntSchema("users", clientName = "users") {
  override fun id() = EntId.long()

  val email by string("email").unique()
  val hashedPassword by string("hashed_password").sensitive()
}

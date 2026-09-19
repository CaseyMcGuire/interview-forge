package com.application.schema

import entkt.schema.EntMixin

/** Creation time stays fixed; updates advance the modification time. */
class Timestamps(scope: EntMixin.Scope) : EntMixin(scope) {
  val createdAt by instant("created_at").defaultNow().immutable()
  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
}

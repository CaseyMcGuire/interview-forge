package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema

class Tag : EntSchema("tags", clientName = "tags") {
  override fun id() = EntId.long()

  val slug by string("slug").unique().immutable()
    .comment("Stable, unique identifier, such as \"two-pointer\" or \"concurrency\".")

  val displayName by string("display_name")
    .comment("Human-readable label, such as \"Two Pointers\" or \"Concurrency\".")

  val problems by manyToMany<Problem>()
    .throughLink<ProblemTag>(ProblemTag::tag, ProblemTag::problem)

  val timestamps = include(::Timestamps)
}

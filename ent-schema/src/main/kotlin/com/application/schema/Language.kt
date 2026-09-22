package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema

/** Shared programming-language catalog, independent of a particular compiler/runtime version. */
class Language : EntSchema("languages", clientName = "languages") {
  /** Database-generated identity used by problem-language configurations. */
  override fun id() = EntId.long()

  val key by string("key").unique().immutable()
    .comment("Stable machine identifier, such as \"python\" or \"kotlin\"; not a runtime version.")

  val displayName by string("display_name")
    .comment("Human-readable name shown in the editor's language selector.")

  val enabled by bool("enabled").default(true)
    .comment("Whether new attempts may use this language; disabling it preserves existing history.")

  val problemConfigurations by hasMany<ProblemLanguage>()
    .comment("Problems that provide starter code for this language.")

  val createdAt by instant("created_at").defaultNow().immutable()
    .comment("Time the language was added to the catalog.")

  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
    .comment("Time its display name or availability was last changed.")
}

package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema

/** Shared programming-language catalog, independent of a particular compiler/runtime version. */
class Language : EntSchema("languages", clientName = "languages") {
  /** Database-generated identity used by problem-language configurations. */
  override fun id() = EntId.long()

  /** Stable machine identifier, such as "python" or "kotlin"; not a runtime version. */
  val key by string("key").unique().immutable()

  /** Human-readable name shown in the editor's language selector. */
  val displayName by string("display_name")

  /** Whether new attempts may use this language; disabling it preserves existing history. */
  val enabled by bool("enabled").default(true)

  /** Problems that provide starter code for this language. */
  val problemConfigurations by hasMany<ProblemLanguage>()

  /** Time the language was added to the catalog. */
  val createdAt by instant("created_at").defaultNow().immutable()

  /** Time its display name or availability was last changed. */
  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
}

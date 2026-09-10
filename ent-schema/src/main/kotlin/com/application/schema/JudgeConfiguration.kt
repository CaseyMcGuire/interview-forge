package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Private execution settings. Sensitive fields are redacted from strings, not access-controlled yet. */
class JudgeConfiguration : EntSchema("judge_configurations", clientName = "judgeConfigurations") {
  // Database-generated identity for this private configuration.
  override fun id() = EntId.long()

  // The single problem/language combination this configuration can judge.
  val problemLanguageId by long("problem_language_id").immutable()

  // Enforces one judge configuration per problem/language pair and preserves its parent reference.
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language").field(problemLanguageId)
    .unique().inverse(ProblemLanguage::judgeConfiguration).onDelete(OnDelete.RESTRICT)

  // Trusted runner configuration identifier, including the runtime version; never a shell command.
  val runtimeKey by string("runtime_key")

  // Private source that deserializes input, invokes the solution, and serializes its output.
  val harnessSource by text("harness_source").sensitive()

  // Private validator for multiple valid answers; absent for EXACT_JSON and required for CUSTOM.
  val checkerSource by text("checker_source").nullable().sensitive()

  // Maximum execution time per case, in milliseconds; must be positive before this judge is usable.
  val timeLimitMs by int("time_limit_ms")

  // Maximum memory per case, in megabytes; must be positive before this judge is usable.
  val memoryLimitMb by int("memory_limit_mb")

  // Time the judge configuration was created.
  val createdAt by time("created_at").defaultNow().immutable()

  // Time the harness, checker, runtime selection, or resource limits were last changed.
  val updatedAt by time("updated_at").defaultNow().updateDefaultNow()
}

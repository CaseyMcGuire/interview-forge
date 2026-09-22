package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Private execution settings; optional reference code prepares expected outputs for custom inputs. */
class JudgeConfiguration : EntSchema("judge_configurations", clientName = "judgeConfigurations") {
  /** Database-generated identity for this private configuration. */
  override fun id() = EntId.long()

  val problemLanguage by belongsTo<ProblemLanguage>("problem_language_id")
    .immutable()
    .unique()
    .inverse(ProblemLanguage::judgeConfiguration)
    .onDelete(OnDelete.RESTRICT)
    .comment("Fixed problem/language pair this configuration judges; each pair has at most one configuration.")

  val testDriverCode by string("test_driver_code").sensitive()
    .comment("Private code that parses test input, calls the submitted solution, and serializes its output.")

  val referenceSolutionCode by string("reference_solution_code").nullable().sensitive()
    .comment("Private solution compiled with this language's driver; null until configured.")

  val checkerSource by string("checker_source").nullable().sensitive()
    .comment("Private validator for multiple valid answers; absent for EXACT_JSON and required for CUSTOM.")

  val timeLimitMs by int("time_limit_ms")
    .comment("Maximum execution time per case, in milliseconds; must be positive before this judge is usable.")

  val memoryLimitMb by int("memory_limit_mb")
    .comment("Maximum memory per case, in megabytes; must be positive before this judge is usable.")

  val createdAt by instant("created_at").defaultNow().immutable()
    .comment("Time the judge configuration was created.")

  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
    .comment("Time the driver, reference solution, checker, or resource limits were last changed.")
}

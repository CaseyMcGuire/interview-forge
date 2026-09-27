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

  val testDriverType by enum<TestDriverType>("test_driver_type").default(TestDriverType.MAIN_FUNCTION)
    .comment("Entry-point contract the runtime uses to invoke the test driver.")

  val testDriverCode by string("test_driver_code").sensitive()
    .comment("Private code that invokes the submitted solution using the selected test-driver contract.")

  val referenceSolutionCode by string("reference_solution_code").nullable().sensitive()
    .comment("Private solution compiled with this language's driver; null until configured.")

  val timeLimitMs by int("time_limit_ms")
    .comment("Maximum execution time per case, in milliseconds; must be positive before this judge is usable.")

  val memoryLimitMb by int("memory_limit_mb")
    .comment("Maximum memory per case, in megabytes; must be positive before this judge is usable.")

  val createdAt by instant("created_at").defaultNow().immutable()
    .comment("Time the judge configuration was created.")

  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
    .comment("Time the driver type, driver code, reference solution, or resource limits were last changed.")
}

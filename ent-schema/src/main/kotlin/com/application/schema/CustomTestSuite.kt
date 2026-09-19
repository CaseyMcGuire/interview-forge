package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Fixed user-supplied inputs, created fresh for each custom test run. */
class CustomTestSuite : EntSchema("custom_test_suites", clientName = "customTestSuites") {
  override fun id() = EntId.long()

  /** Derived from the authenticated user; the test run inherits ownership through this suite. */
  val user by belongsTo<User>("user")
    .immutable()
    .onDelete(OnDelete.RESTRICT)

  /** Determines both the problem and language used to prepare and execute these inputs. */
  val problemLanguage by belongsTo<ProblemLanguage>("problem_language")
    .immutable()
    .inverse(ProblemLanguage::customTestSuites)
    .onDelete(OnDelete.RESTRICT)

  val cases by hasMany<CustomTestCase>("cases")
  val run by hasOne<CustomTestSuiteRun>("run")

  /** Cleanup may delete the suite after this time, once its test run finishes. */
  val expiresAt by instant("expires_at").immutable()

  val createdAt by instant("created_at").defaultNow().immutable()

  /** Supports owner-scoped admission checks through each suite's test run. */
  val byUser = index("idx_custom_test_suites_user", user.fk)

  val byProblemLanguage = index("idx_custom_test_suites_problem_language", problemLanguage.fk)

  /** Supports finding expired suites without scanning retained inputs and results. */
  val byExpiresAt = index("idx_custom_test_suites_expires_at", expiresAt)
}

package com.application.schema

import entkt.schema.EntMixin

/** Source and execution summary shared by problem submissions and custom input submissions. */
class ExecutionAttemptFields(scope: EntMixin.Scope) : EntMixin(scope) {
  /** Exact submitted source, retained independently of later editor changes. */
  val sourceCode by string("source_code").immutable()

  /** Selected case count; official submissions set it when the worker selects their cases. */
  val totalCases by int("total_cases")

  /** Number of passed cases, between zero and totalCases. */
  val passedCases by int("passed_cases").default(0)

  /** Measured user suite runtime; excludes compilation and reference execution. */
  val runtimeMs by long("runtime_ms").nullable()

  /** Safe failure explanation; exclude hidden inputs and private execution diagnostics. */
  val publicErrorMessage by string("public_error_message").nullable()

  /** Null while queued; set when a worker starts execution. */
  val startedAt by instant("started_at").nullable()

  /** Null until terminal completion, including compilation and infrastructure failures. */
  val finishedAt by instant("finished_at").nullable()
}

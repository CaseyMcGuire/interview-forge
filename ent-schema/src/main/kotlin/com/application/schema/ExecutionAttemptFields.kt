package com.application.schema

import entkt.schema.EntMixin

/** Source and execution summary shared by problem submissions and custom input submissions. */
class ExecutionAttemptFields(scope: EntMixin.Scope) : EntMixin(scope) {
  val sourceCode by string("source_code").immutable()
    .comment("Exact submitted source, retained independently of later editor changes.")

  val totalCases by int("total_cases")
    .comment("Selected case count; official submissions set it when the worker selects their cases.")

  val passedCases by int("passed_cases").default(0)
    .comment("Number of passed cases, between zero and totalCases.")

  val runtimeMs by long("runtime_ms").nullable()
    .comment("Measured user suite runtime; excludes compilation and reference execution.")

  val publicErrorMessage by string("public_error_message").nullable()
    .comment("Safe failure explanation; exclude hidden inputs and private execution diagnostics.")

  val startedAt by instant("started_at").nullable()
    .comment("Null while queued; set when a worker starts execution.")

  val finishedAt by instant("finished_at").nullable()
    .comment("Null until terminal completion, including compilation and infrastructure failures.")
}

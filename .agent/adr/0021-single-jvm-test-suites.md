# 0021: One JVM for each submission's test suite

Date: 2026-09-17

Status: Implemented, reviewed, and committed with the Docker runtime stage.

## Decision

Compile a submission once, then start one JVM with all ordered test cases. Return either
all passed or the first failure, including its zero-based index and number of preceding
passes. Do not start later cases after a failure or retain successful case outputs.

Package `KotlinTestSuite` in the runtime image using the `kotlin-runtime` Gradle subproject.
It shares `JsonOutputChecker` with the application and reuses the existing JSON libraries.
Launch it with `TestDriverKt` as an explicit argument, preserving the selection of the
private driver even if the solution defines its own `main`.

Keep the driver's single-input contract: a top-level `main()` or `main(args: Array<String>)`
in the default package reads one JSON input and prints one JSON output. Invoke that same
method for each case using one class loader, rebinding stdin/stdout/stderr between calls.
Static fields, caches, allocations, and threads remain live across cases. Resetting them
would conceal state leaks that later cases should expose.

Pass the complete suite, including expected outputs, as JSON through the temporary input
mount. Grade each case inside the JVM with the existing strict JSON comparison. Emit a
small case-start checkpoint before each invocation, followed by one terminal result.
The application uses checkpoints to identify the active case after an abrupt exit or OOM;
a zero exit code without a complete result is a runtime failure.

## Limits and cleanup

Each driver invocation has its configured time limit and independent 20,000-byte stdout
and stderr limits. A watchdog reports a timeout and halts the JVM; interrupting a looping
solution thread would not reliably stop it. Completion and the deadline synchronize so
a completed case's watchdog cannot terminate the next case. First failures also halt
after reporting, preventing leftover solution threads from delaying the result.

The suite shares the configured container memory limit. The outer container deadline is
`min(caseCount * timeLimitMs + 2,000, 600,000)` milliseconds, bounding startup, grading,
and leftover threads. After an all-pass report, allow normal JVM shutdown and require a
successful container exit. A leaked non-daemon thread therefore causes a timeout.

Reserve 256,000 bytes for the terminal JSON report and 64 bytes per case checkpoint,
since JSON escaping can expand the two bounded failure streams. Raw container stderr
remains capped at 20,000 bytes. Containers and temporary inputs retain the existing
cleanup and restart-recovery behavior.

## Consequences

Build the Gradle runtime distribution before rebuilding the Kotlin Docker image.
The submission runner calls `runTestSuite` once and returns its result. The scheduler
passes that result to the submission service, which persists its summary and at most
the first failure. Public-example result access remains a separate review stage.
This decision adds no queued case snapshots or passing results.

Expected answers and grading code now share a container and JVM with the solution.
This fits the personal application but is not a tamper-proof grading boundary.
Container restrictions still isolate execution from application credentials and the host.
Recorded runtime covers the suite container, including JVM startup; peak memory remains
unmeasured. A failure before the first checkpoint has no failed-case index.

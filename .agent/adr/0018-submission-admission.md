# 0018: Official submission admission and transient example Runs

Date: 2026-09-16

Status: Accepted design. Official admission/polling and the worker are reviewed and committed;
the transient example/custom Run flow is not implemented. Supersedes the
submission runtime snapshot portion of [0016](0016-language-runtime.md).

## Decision

The current API/backend handles official submissions only. `submitSolution` takes the
problem-language ID and source code and creates a persisted SUBMIT attempt. Official
submissions execute asynchronously and support owner-only polling after browser refresh.
Do not accept custom cases or create persisted example Runs through this API.

Use PostgreSQL as the durable queue. Check global/per-user active SUBMIT capacity and
create the attempt in one serializable EntKt transaction without retries. Concurrent
requests may surface serialization errors; revisit retries if contention becomes an
issue. The app currently serves a single user.
The internal execution identity can read official cases, including hidden tests, and
judge settings without an administrator identity or authentication privacy bypass.
It can manage submission state but cannot change tests/judges or read credentials.

Admission checks that official cases exist but writes no input snapshots or pending case
results. The worker will select the current official suite when execution starts, set
`totalCases`, and keep the selected inputs in memory. Runtime, driver, checker, and resource
limits come from the current problem-language settings and application configuration;
do not persist configuration snapshots.

Persist the execution summary and at most the first failed case. Do not retain successful
case outputs. Compilation errors belong to the summary without a failed-case row. The
first-failure storage model and its privacy-safe response will be implemented with worker
result persistence; the current admission path only writes submission summaries.

Example/custom execution is a separate follow-up. It should run and return results in
one request without submission rows, result history, or polling. Refreshing the page
would lose those results. Saving user-authored sample inputs is another optional feature;
it does not require retaining execution snapshots or outcomes.

## Consequences

The previous queued Run GraphQL contract is replaced with official submission admission
and polling. Remove custom-case DTOs/parsing, example-only selection, snapshot creation,
and per-case Run response/privacy/validation code. Keep durable admission concurrency
protections and owner polling for real submissions.

Docker execution and activation remain a separate review stage; runtime availability
is described in [0019](0019-local-docker-execution.md).
The applied V8 migration stays in history; later physical
schema changes use a new migration. Review stages are recorded in
[the execution proposal](../../docs/submission-execution.md).

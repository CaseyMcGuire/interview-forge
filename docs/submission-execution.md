# Official submission execution: API and backend

Status: The official submission API and admission/polling backend are reviewed and
committed. They supersede the earlier Run API commit `0f8efe3`. The execution worker
is the next review stage and is not implemented.

## Current scope

`submitSolution` accepts a problem-language ID and source code. It creates a persisted
SUBMIT attempt and returns its ID and current summary. `submission(id)` polls an official
submission owned by the authenticated user, including after refresh or problem archival.
There is no example/custom Run mutation, custom-case input, or transient execution path.

Official execution uses the problem's stored examples and hidden tests. Admission verifies
that at least one official case exists but does not select or copy the suite, create pending
case results, or impose the former custom-Run limit of twenty cases. `totalCases` is zero
while queued; the worker sets it when it selects the current suite at execution start.

Example/custom Runs are a separate follow-up: execute and return results directly, with
page state lost on refresh. Saving user-authored sample inputs would be another optional
feature. Neither flow belongs in the current submission API/backend.

## API contract

The complete contract is in [submitSolution.graphql](../src/main/resources/schema/submitSolution.graphql)
and [submission.graphql](../src/main/resources/schema/submission.graphql).

- Input contains only `problemLanguageId` and `sourceCode`. Require nonblank source of at
  most 50,000 characters and retain it without trimming.
- Expected failures are authentication required, problem not found/unavailable, invalid
  input, execution unavailable, and execution busy. They create no submission.
- Successful admission returns a persisted submission, normally QUEUED. The worker may
  already have advanced its state before the response is read.
- Polling exposes ID, lifecycle, overall verdict, total/passed counts, measurements,
  timestamps, and a safe error message. It returns null for malformed, wrong-type,
  missing, unauthenticated, other-owner, or non-SUBMIT IDs.
- Official verdicts include ACCEPTED and WRONG_ANSWER. Example-only EXECUTED verdicts and
  per-case Run response types have been removed from GraphQL.
- Hidden input/output and private judge details are not part of the summary response.
  Expected request failures use the mutation union; unexpected database/application
  failures remain GraphQL errors.

## Admission and polling backend

1. Authenticate the caller and resolve the problem-language ID.
2. Load the public problem-language configuration, problem, and enabled language through
   EntKt. Verify supported Kotlin/EXACT_JSON execution, a judge configuration, a configured
   runtime, an available execution adapter, and a nonempty official suite.
3. In the same serializable transaction, check global/per-user queued/running SUBMIT limits
   and create the official submission with its owner, problem-language reference, and source.
   EntKt validates source with `SubmissionContentValidationRule` when saving; the resolver maps
   violations to field errors. Availability and capacity checks precede source validation.
   Admission defaults remain 100 active submissions globally and 2 per user.
4. Make one transaction attempt. Concurrent requests may cause serialization failures,
   which propagate as errors. Revisit retries if contention becomes an issue; the app
   currently serves a single user.
5. Poll the persisted summary through the owner privacy policy. Ordinary viewers cannot
   create or update execution state, and administrators cannot read another user's submission.

An internal execution identity can read judge configurations and official test cases,
including hidden inputs, and manage submission lifecycle. It cannot read credentials,
change test inputs or judge settings, or delete submissions. Ordinary test-case privacy
rules remain in force for public requests. Tests use EntKt for fixtures and assertions.

`RuntimeAvailability` is a read-only adapter check. There is no production adapter yet,
so admission returns ExecutionUnavailable until the execution stage installs one. Tests
supply an available adapter to exercise queue creation and concurrency. No fake production
worker accepts work it cannot execute.

## Worker and result retention

The next execution stage will atomically claim queued submissions, record RUNNING, and
load current judge settings, configured runtime, and the official suite. Keep those inputs
in memory while executing and keep database transactions outside compilation/process work.
Do not persist runtime, driver, checker, resource-limit, or full-suite snapshots.

Persist the execution summary and at most the first failed case; successful case outputs
are not retained. Admission currently writes no case results. The existing case-result
schema remains unchanged in this removal; implementing first-failure-only storage and
its privacy-safe result response belongs with the worker's result persistence.
Compilation failures require a summary error but no failed-case row.

The worker must persist terminal status, bound runtime resources and outputs, and recover
interrupted work without leaving submissions permanently RUNNING. Retained state and
polling survive browser refresh and are independent of the original admission request.

## Runtime boundary

`LanguageExecutionConfig` describes source files and commands. The runtime executor owns
isolated compilation, process lifecycle, stdin/stdout/stderr, limits, and cleanup. Start
with Kotlin and exact JSON comparison. Select `execution.runtimes[language.key]` and current
judge settings when the worker starts each attempt.

Run submitted code outside the web process with no application credentials or network
access, restricted mounts, and bounded CPU/memory/process/output use. Enforce deadlines
and clean up on all exit paths. Send one JSON value on stdin and require exactly one JSON
value on stdout. Keep stderr bounded. Compare outputs outside the submitted program and
never pass expected answers or private checker code into it.

Only record available measurements. Preserve JSON numeric values during comparison;
object key order and whitespace do not matter, array order and JSON types do. Compilation,
runtime, time/memory limits, output errors, and infrastructure failures need explicit
outcomes, with safe diagnostics excluding hidden inputs.

## Review stages

- [x] **Official submission API:** Replace the previous Run mutation with `submitSolution`,
  accept only source and problem-language ID, and retain owner-only submission polling.
  API/code generation are updated, reviewed, and committed.
- [x] **Submission backend:** Persist official attempts with transactional admission,
  source validation, scoped execution access, and owner polling. Remove example/custom
  selection, case snapshots, and Run-only validation. Implemented, reviewed, and committed.
- [ ] **Execution backend:** Implement worker claims, the isolated Kotlin executor,
  comparison, first-failure-only result persistence, limits, cleanup, and interrupted-work
  recovery. Validate complete official submissions end to end.

Each stage is reviewed before committing. Example/custom execution and frontend integration
remain follow-ups. V8 has already been applied locally; later physical schema changes
require a new Flyway migration. The mutable `totalCases` metadata change does not alter
its physical database column.

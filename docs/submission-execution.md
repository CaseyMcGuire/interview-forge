# Official submission execution: API and backend

Status: The official submission API and admission/polling backend are reviewed and
committed. They supersede the earlier Run API commit `0f8efe3`. JSON output checking is
committed in `4d3d81d`. The submission worker and execution interfaces are awaiting review;
the Docker/JVM implementation is stashed so the caller can be reviewed first.

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

`SubmissionWorker.executeNextSubmission()` claims the oldest queued official submission,
records RUNNING, and loads current judge settings, runtime configuration, and ordered cases.
It prepares and compiles the program, calls `runTestSuite` once, then saves the result.
Inputs stay in memory while executing; database transactions remain outside compilation
and suite execution. No runtime, driver, checker, limit, or full-suite snapshots are persisted.

The summary and at most the first failed case are saved together in one transaction.
The returned zero-based failure index selects the original case, including its input,
expectation, position, and visibility. Later test edits cannot change that retained data.
Passing outputs are not retained. Compilation failures and JVM failures before any case
starts have no failed-case row. Suite timing belongs to the summary; individual case
timing stays null because the executor does not measure it.

Only the execution identity can create or read failed-case records in this stage.
Public-example access is a separate API stage. Public errors exclude private diagnostics.
The existing owner polling API exposes RUNNING and the terminal summary; there are no
per-case progress writes during the suite.

The worker cleans abandoned executor resources before its first attempt and finishes
leftover RUNNING rows as INTERNAL_ERROR before claiming more work. This assumes one
worker for the application. Exceptions close the prepared program and finish the attempt;
interruptions also propagate to the caller with the thread's interruption flag restored.
Final database writes are not retried because a lost connection can leave their commit
outcome unknown. A remaining RUNNING row is recovered before the next attempt.

For this review, the worker is constructor-injected and not registered or scheduled in
Spring. Integration tests use a fake `ProgramExecutor`, a real PostgreSQL database, and
the existing submission/polling API. Production activation belongs with the runtime
stage; admission remains unavailable while its implementation is stashed.

## Runtime boundary

`LanguageExecutionConfig` describes source files and commands. The runtime executor owns
isolated compilation, process lifecycle, stdin/stdout/stderr, limits, and cleanup. Start
with Kotlin and exact JSON comparison. Select `execution.runtimes[language.key]` and current
judge settings when the worker starts each attempt.

`ProgramExecutor.prepareProgram` returns a `ProgramExecution` that the worker closes
after compilation and execution. `runTestSuite` accepts all ordered inputs and expected
outputs together, plus the case time limit and shared memory limit. `TestSuiteResult`
reports all passed or the first failure, its index, passed count, bounded failure output,
and optional suite duration. The worker maps that result to persisted verdicts without
parsing process output or performing comparisons.

The stashed implementation runs one JVM for the whole suite, preserving state between
cases. It enforces limits, performs strict JSON comparison, and reports the first failure.
Its containers run without application credentials or network access. Expected outputs
are available inside the submission JVM; this is not a tamper-proof grading boundary.

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
- [x] **JSON output checking:** Strict JSON parsing and comparison, reviewed and committed.
- [ ] **Submission worker and execution API:** Claim submissions, load current settings,
  compile, run the suite once, and persist the summary and first failure. Implemented
  with fake-executor integration tests; awaiting review.
- [ ] **Docker/JVM runtime and activation:** Restore the implementation, scheduling,
  default runtime mapping, and admission wiring; validate the complete path. Stashed/deferred.
- [ ] **Failed-example API:** Owner-only public-example results and privacy tests. Stashed.

The complete runtime is saved in stash `9d4a201` ("Single-JVM Docker runtime before
submission worker API review"). Earlier implementation backups remain intact. Restore
individual files after reviewing the worker, rather than applying the entire old diff.
`TestSuiteResult` currently lives beside the worker's interfaces; the runtime stage will
move the shared contract into its Gradle module. Reconcile any API changes before restoring
the caller wiring, and keep the original worker draft's per-case loop out of the final code.

Each stage is reviewed before committing. Example/custom execution and frontend integration
remain follow-ups. V8 has already been applied locally; later physical schema changes
require a new Flyway migration. The mutable `totalCases` metadata change does not alter
its physical database column.

After the execution work is complete, remove the mandatory ADR rule and the new execution
ADRs as a separate follow-up. Existing ADRs remain as history.

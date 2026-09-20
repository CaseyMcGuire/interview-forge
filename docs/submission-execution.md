# Official submission execution: API and backend

Status: The official submission API and admission/polling backend are reviewed and
committed. They supersede the earlier Run API commit `0f8efe3`. JSON output checking is
committed in `4d3d81d`. The submission worker and execution interfaces are reviewed and
committed in `e403ec5`. Docker/JVM execution and scheduled activation are reviewed and committed.
The failed-example API is reviewed and committed.

## Current scope

`submitSolution` accepts a problem-language ID and source code. It creates a persisted
official attempt and returns its ID and current summary. `submission(id)` polls an official
submission owned by the authenticated user, including after refresh or problem archival.
Custom requests use the separate persisted `enqueueCustomTestSuiteRun` and `customTestSuiteRun` API below.

Official execution uses the problem's stored examples and hidden tests. Admission verifies
that at least one official case exists but does not select or copy the suite, create pending
case results, or impose the former custom-Run limit of twenty cases. `totalCases` is zero
while queued; the service sets it when it selects the current suite at execution start.

Custom-run admission and owner polling are reviewed and committed. Execution,
reference-output preparation, scheduling, expiration cleanup, and frontend integration
remain later stages. Custom runs currently stay queued.

## Grading-job design and review plan

Official admission will create the submission and a ready grading job in one transaction,
using the problem's stored inputs and expected answers. Custom admission will save only
the queued run and its cases. A background worker will execute the reference solution, then
save the generated expectations and create the grading job together. Both paths will use
the same `CodeGrader`; all compilation and execution stay outside API requests and database
transactions. No separate expected-output job table is needed.

A grading job holds the submitted source, ordered input/expected-output snapshots, the
problem-language reference, and exactly one originating attempt. Case metadata supports
retaining the first official failure or all custom results. Runtime selection stays in
application configuration. Saving the final result and deleting the job will be atomic.

The `cases` field is a typed `List<GradingCase>` serialized into the existing JSONB column.
Each entry requires a case ID, JSON input, and JSON expected output; JSON null remains a
valid answer. Optional `TestCaseVisibility` records the original visibility of official
cases and is absent for custom cases. Array order determines execution order.

Grading-job storage and the consolidation of custom suites into runs are reviewed and
committed. Later work is preserved in
stash `875135cbbb210a6577b68e0a5fb5578429c535d5`, named
`Custom test suite workflow: later review stages after grading-job storage`. Reapply only
the files or hunks needed for each stage; do not restore the entire stash at once. The
superseded standalone custom runner was removed before this stash was created.
The stash predates the consolidation: adapt its suite lookups to run-owned cases when
reapplying each stage.

Review and commit one slice at a time. Tests and necessary generation belong with the
code they validate. The remaining stages split storage, grading, runtime integration,
and the two queue producers into separate reviews:

- [x] **1. Official submission model:** Summary and first-failure retention.
- [x] **2. GraphQL contract:** Enqueue and poll custom runs.
- [x] **3. Custom execution schemas:** Owned suites, cases, and runs.
- [x] **4. Backend services and access rules:** Admission, validation, and owner polling.
- [x] **5. Grading-job storage:** Schema, origin constraints, execution-only access, V15
  migration, EntKt generation, and storage tests. Also consolidates custom suites into
  runs through V16 and updates admission, polling, and ownership.
- [x] **6. Custom result storage:** Result payloads, safe messages, finalization,
  polling mapping, and persistence tests.
- [x] **7. Shared grading:** Common grading input/result types, `CodeGrader`, and its
  `CodeExecutionService` contract. Test comparison and execution outcomes with a fake
  execution service, independently of Docker and database access.
- [ ] **8. Docker execution:** Implement the shared execution contract, collect every
  reachable case's output in one JVM, and update the protocol reader and current official
  caller together. Include lifecycle, output-limit, and Docker tests; rebuild the image.
- [ ] **9. Official grading-job flow:** Create jobs during official admission, process
  them through the shared grader, and atomically retain the summary/first failure and
  delete the job. Replace the old official runner/scheduler path and test recovery.
- [ ] **10. Custom reference preparation:** Claim queued custom runs, generate expected
  outputs asynchronously, and atomically enqueue their grading jobs. Route their grading
  results to custom storage and test preparation failures, recovery, and polling.
- [ ] **11. Expiration cleanup:** Delete expired custom runs and their cases after execution finishes.
- [ ] **12. Frontend components:** Custom-input editing and per-case result views.
- [ ] **13. Frontend integration:** Connect enqueue/polling and validate complete flows.

Stage 5 adds `GradingJob`, `GradingCase`, its status enum and two inverse edges,
the policy and validator, policy registration, migration V15, and `GradingJobIntegrationTest`.
The schema module enables the Kotlin serialization compiler plugin for typed JSON payloads. Jobs have
QUEUED/RUNNING status and creation/start timestamps. Foreign keys, unique indexes, and
an exclusive-origin check enforce their destination; deleting that destination cascades
to the job. Access to grading jobs is controlled by the execution-only privacy policy.
The existing admission and workers do not create or consume jobs yet.

Each custom request already creates a fresh set of inputs for one run, so the separate
`CustomTestSuite` entity has been removed. `CustomTestSuiteRun` now owns the user,
problem-language reference, expiration, and cases directly. V16 transfers these fields
and reparents cases while preserving run and case IDs, retained results, and grading-job
references. The public GraphQL contract is unchanged.

Stage 6 restores custom result storage from the stash using the run's direct case
relationship. `CustomTestSuiteRunService.finishCustomTestSuiteRun` locks a running
attempt and saves its summary and ordered result array together. It rejects duplicate
or foreign case IDs, invalid durations, incomplete passing results, and attempts to
finish a queued or already-finished run. Missing case results become `NOT_RUN`.

`CustomTestCaseResult` handles encoding and decoding each stored result, with only
the case ID, outcome, and user-program output. Output is capped at 20,000 characters
and NUL characters are replaced for PostgreSQL JSONB compatibility. Public messages
come from outcome mappings rather than program diagnostics. The polling mapper still
uses generated DGS types. `TestCaseOutcome` is in its own file; later grading stages
should reuse it rather than reintroduce the enum from the stashed `TestCaseResult.kt`.

Validation: `./gradlew test --tests '*CustomTestSuiteRunPersistenceIntegrationTest'
--tests '*CustomTestSuiteRunIntegrationTest'` passed all 19 tests. This stage exercises
storage and polling with supplied results; runtime execution and job consumption
remain later stages. No schema or generated-artifact changes are required.

Stage 7 introduces `CodeGrader.gradeCode`, which accepts a prepared program, ordered
`TestCaseInput` values, and execution settings. It has no EntKt, GraphQL, or attempt-type
dependency. `CodeExecutionService.executeCode` owns compilation, running all inputs in
one process, and cleanup. It receives only inputs; expected answers stay with the grader.
The same execution contract will let reference preparation collect outputs without grading.

`CodeExecutionResult` distinguishes compilation failure from completed execution and
keeps the process status separate from individual cases. Its `caseResults` contain
`TestCaseExecutionResult` values with the JSON input, nullable parsed JSON answer,
`ProgramStatus`, stdout, stderr, and nullable per-case runtime. Kotlin null means no
valid answer was produced; `JsonNull` represents a valid JSON null answer. The execution
implementation must parse answers without losing numeric type or precision.

`GradingResult` contains one `TestCaseGradingResult` per supplied case, its overall outcome,
and the suite duration. Each graded case holds its outcome and the original execution
result, which owns the input, parsed answer, process status, streams, and measured runtime.
Unrun cases have no execution result; their position matches the supplied case list.
The grader verifies that returned inputs match the requested
order and compares every structured answer separately from stdout. It fills unreached
cases with `NOT_RUN` and rejects missing results from a reportedly successful process.
A failed process exit overrides matching
case outputs. Strict JSON comparison preserves numeric types and precision; output caps
count UTF-8 bytes for JSON answers and both streams.

This replaces the stashed `ProgramRunner` and `TestSuiteGrader` design. Do not reapply
those classes or their old result wrappers when adapting the Docker stage. The new
execution contract has only a test fake at this point; Spring wiring and its Docker
implementation belong to stage 8. The existing official worker continues to use its
current execution path until that integration is reviewed.

Validation: `./gradlew test --tests '*CodeGraderTest' --tests '*JsonOutputCheckerTest'`
passed all 17 unit tests without Docker or database access. Docker lifecycle and
production wiring are not exercised in this stage. No generated artifacts changed.

## Custom test suite admission and polling

`enqueueCustomTestSuiteRun` accepts a problem-language ID, source code, and ordered JSON inputs. Each
accepted request creates a new owned `CustomTestSuiteRun` and its `CustomTestCase` rows
in a serializable transaction. Invalid requests roll back the run and its cases together.
Source and stored case content use EntKt validation; request validation handles
the case count and decoding JSON. Expected outputs start absent and will be prepared
by the runner using the private reference solution on `JudgeConfiguration`.

Admission requires an available public problem and language, a configured runtime,
an EXACT_JSON judge, and nonblank reference code. Custom runs do not require official
test cases. `ExecutionAvailabilityService` applies the existing global and per-user limits
to queued/running attempts across both official submissions and custom runs. Both
admission paths read those counts in the transaction that creates the attempt.

`execution.max-custom-test-cases` defaults to 20. `execution.custom-test-suite-lifetime`
defaults to 5 minutes and sets the run's expiration when it is created. Expiration is
metadata until the cleanup stage is implemented; it does not make retained data unreadable.

`customTestSuiteRun(id)` returns only the authenticated owner's attempt, including after
problem archival. Run ownership governs the run and its cases. Ordinary viewers,
including administrators, cannot read someone else's inputs/results or mutate execution
state. Execution may create runs and cases, prepare expectations, and update runs;
deletion remains disabled until the cleanup stage.

Polling maps the retained result JSON to generated DGS types and returns each case in
input order. SQL null expected output means unprepared; JSON null is returned as the
string `"null"`. Reference code, compiler diagnostics, and official hidden cases are not
part of this response. This stage uses persisted result fixtures to verify polling;
the runner will supply those results in later stages.

## API contract

The complete contract is in [submitSolution.graphql](../src/main/resources/schema/submitSolution.graphql)
and [submission.graphql](../src/main/resources/schema/submission.graphql).

- Input contains only `problemLanguageId` and `sourceCode`. Require nonblank source of at
  most 50,000 characters and retain it without trimming.
- Expected failures are authentication required, problem not found/unavailable, invalid
  input, execution unavailable, and execution busy. They create no submission.
- Successful admission returns a persisted submission, normally QUEUED. The scheduler may
  already have advanced its state before the response is read.
- Polling exposes ID, lifecycle, overall verdict, total/passed counts, measurements,
  timestamps, and a safe error message. It returns null for malformed, wrong-type,
  missing, unauthenticated, or other-owner IDs.
- Official verdicts include ACCEPTED and WRONG_ANSWER. Example-only EXECUTED verdicts and
  per-case Run response types have been removed from GraphQL.
- Hidden input/output and private judge details are not part of the summary response.
  Expected request failures use the mutation union; unexpected database/application
  failures remain GraphQL errors.
- `failedExample` exposes the retained JSON input, JSON expectation, and captured
  stdout when the first failure was a public example. It is null before completion, when
  no case failed, or when the first failure was hidden. Output may be empty or invalid JSON;
  stored NUL characters are replaced with the Unicode replacement character. No stderr,
  private compiler diagnostics, or unmeasured per-case timing is exposed.

## Admission and polling backend

1. Authenticate the caller and resolve the problem-language ID.
2. Load the public problem-language configuration, problem, and enabled language through
   EntKt. Verify supported Kotlin/EXACT_JSON execution, a judge configuration, a configured
   runtime, an available execution adapter, and a nonempty official suite.
3. In the same serializable transaction, check global/per-user queued/running submission limits
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

`DockerExecutionService` supplies `RuntimeAvailability` by inspecting the configured
local image. Missing Docker or a missing image returns ExecutionUnavailable; admission
never pulls images or installs tools. Tests can substitute availability to exercise
admission without starting executions. The default Kotlin image is configured in
`application.yaml` and must be built before submitting solutions locally.

## Submission execution and result retention

`SubmissionScheduler` coordinates claiming, running, and finishing one submission.
`SubmissionService.claimNextQueuedSubmission()` atomically claims the oldest queued
official submission and returns it with RUNNING status. The service also loads current
judge settings, runtime configuration, and ordered cases, and saves final outcomes.
`SubmissionRunner.runSubmission()` loads those settings through the service, prepares
and compiles the program, calls `runTestSuite` once, and returns a result after closing
the program. The scheduler passes that result to `SubmissionService.finishSubmission()`.
Inputs stay in memory while executing; database transactions remain outside compilation
and suite execution. No runtime, driver, checker, limit, or full-suite snapshots are persisted.

The summary and at most the first failed case are saved together in one transaction.
`Submission.failedTestResult` is an optional one-to-one edge to `SubmissionFailure`.
The `submission_failures.submission_id` unique constraint prevents retaining more than
one failure for an attempt. `Submission` represents
official attempts only and has no run/submit discriminator.
The returned zero-based failure index selects the original case, including its input,
expectation, and visibility. Later test edits cannot change that retained data.
Case positions remain on `TestCase` and are not copied into retained failures.
Passing outputs are not retained. Compilation failures and JVM failures before any case
starts have no failed-case row. Suite timing belongs to the summary; individual case
timing stays null because the executor does not measure it.

Only the execution identity can create failed-case records. The authenticated submission
owner may read a failed example from a finished official submission; hidden and custom
records remain execution-only. The EntKt privacy rule uses the retained visibility, so a
hidden test made public later does not expose old failures. A retained public example stays
available to its owner after test edits or problem archival. Other users and administrators
cannot read it. Public errors exclude private diagnostics.
The existing owner polling API exposes RUNNING and the terminal summary; there are no
per-case progress writes during the suite.

The scheduler cleans abandoned executor resources before its first attempt and finishes
leftover RUNNING rows as INTERNAL_ERROR before claiming more work. This assumes one
scheduler for the application. Exceptions close the prepared program and finish the attempt;
interruptions also propagate to the caller with the thread's interruption flag restored.
Final database writes are not retried because a lost connection can leave their commit
outcome unknown. A remaining RUNNING row is recovered before the next attempt.

`SubmissionScheduler` checks for queued work every second after application startup,
provided a configured runtime is available. Calls are sequential, with a delay after
each attempt. `execution.worker-enabled=false` pauses automatic processing; tests use
that setting and invoke the runner or scheduler directly. Fake-executor tests cover
failure handling, while real Docker tests exercise compilation and suite results through
the submission/polling API.

`prepareProgram` receives the submission ID. Docker attaches both a workspace ownership
label and a submission label when creating compilation and suite containers. Recovery
can find leftovers even if the app crashes immediately after creation; container IDs
are not stored in submission rows. Startup cleanup removes owned containers and temporary
workspaces before interrupted database rows are finished. If cleanup fails, those rows
stay RUNNING and the next scheduled call retries recovery. Only one application instance
may manage a database/workspace.

## Runtime boundary

`LanguageExecutionConfig` describes source files and commands. The runtime executor owns
isolated compilation, process lifecycle, stdin/stdout/stderr, limits, and cleanup. Start
with Kotlin and exact JSON comparison. Select `execution.runtimes[language.key]` and current
judge settings when the runner starts each attempt.

`ProgramExecutor.prepareProgram` returns a `ProgramExecution` that the runner closes
after compilation and execution. `runTestSuite` accepts all ordered inputs and expected
outputs together, plus the case time limit and shared memory limit. `TestSuiteResult`
reports all passed or the first failure, its index, passed count, bounded failure output,
and optional suite duration. The runner maps that result to submission verdicts without
parsing process output or performing comparisons.

The Docker implementation runs one JVM for the whole suite, preserving state between
cases. It enforces limits, performs strict JSON comparison, and reports the first failure.
Its containers run without application credentials or network access. Expected outputs
are available inside the submission JVM; this is not a tamper-proof grading boundary.

Only record available measurements. Preserve JSON numeric values during comparison;
object key order and whitespace do not matter, array order and JSON types do. Compilation,
runtime, time/memory limits, output errors, and infrastructure failures need explicit
outcomes, with safe diagnostics excluding hidden inputs.

`KotlinTestSuite` invokes the existing driver's top-level `main()` or `main(args)` for each
case in the same JVM and class loader. Only stdin/stdout/stderr are rebound between calls.
Driver invocations retain the configured per-case deadline and independent 20,000-byte
stdout/stderr limits. A watchdog halts a looping case; the container's outer deadline is
`min(caseCount * timeLimitMs + 2,000, 600,000)` milliseconds. An all-pass report also requires
a successful JVM exit, so leftover non-daemon threads can cause a timeout.

The Docker sandbox uses an unprivileged user, a read-only root, dropped capabilities,
one CPU, 128 processes, a 64 MiB temporary filesystem, and the configured shared memory
limit. Compilation has a separate 60-second/1 GiB budget. Source files are removed after
compilation, and the suite's program mount is read-only. A temporary read-only suite input
file supplies stdin and EOF. Container-side deadlines continue if the application stops.

The suite emits a case-start checkpoint before each invocation and one terminal report.
The host reserves 256,000 bytes for the escaped failure report plus 64 bytes per checkpoint.
Checkpoints identify the active case after an abrupt JVM exit. All containers, temporary
inputs, and program workspaces are removed on normal completion, failure, or interruption;
startup recovery handles leftovers. Docker availability is required for removal.

## Local runtime setup

With Docker running, build the standalone driver distribution and image:

```sh
./gradlew :kotlin-runtime:installDist
docker build -t interview-forge-kotlin:2.4.20 runtime/kotlin
```

Rebuild the image after changing the suite driver or JSON checker. It contains Kotlin
2.4.20, Java 21, GNU timeout, and the shared JSON libraries. The application uses
`docker-java` and its standard Docker context/environment settings; execution does not
launch the Docker CLI or download images. `execution.workspace-directory` defaults to
`interview-forge-execution` under the system temporary directory and must be accessible
to the local Docker daemon. Reserve it for this application.

## Review stages

- [x] **Official submission API:** Replace the previous Run mutation with `submitSolution`,
  accept only source and problem-language ID, and retain owner-only submission polling.
  API/code generation are updated, reviewed, and committed.
- [x] **Submission backend:** Persist official attempts with transactional admission,
  source validation, scoped execution access, and owner polling. Remove example/custom
  selection, case snapshots, and Run-only validation. Implemented, reviewed, and committed.
- [x] **JSON output checking:** Strict JSON parsing and comparison, reviewed and committed.
- [x] **Submission worker and execution API:** Claim submissions, load current settings,
  compile, run the suite once, and persist the summary and first failure. Reviewed and
  committed in `e403ec5`.
- [x] **Docker/JVM runtime and activation:** Docker API implementation, one-JVM suite,
  container labels and recovery, scheduling, default runtime mapping, and admission wiring.
  Implemented, reviewed, and committed.
- [x] **Failed-example API:** Owner-only public-example results and privacy tests.
  Implemented, reviewed, and committed.

Stash `9d4a201` preserves the runtime before the worker review; earlier backups remain
intact. The active implementation has been reconciled with the reviewed worker API.
`TestSuiteResult` and `JsonOutputChecker` now live in the shared `kotlin-runtime` module.
The failed-example API has been reapplied against the current service and suite execution
model. The backups remain intact; their old worker drafts must not replace the current
scheduler, service, or runner.

Each stage is reviewed before committing. Custom execution remains a follow-up, along
with skipping locked queue rows and recovering submissions stuck in RUNNING beyond
their allowed execution window. V9 renames the failure table in place, enforces one
failure per submission, and removes `submissions.kind`. It rejects databases with
non-official attempts rather than silently reclassifying them; existing duplicate
result rows must also be resolved before the unique constraint can be added.
V10 removes the copied case position from retained failures.

The ADR cleanup follow-up is reviewed and committed: submission execution ADRs
0018–0021 have been removed, earlier ADRs remain as history, and new ADRs are created only
when explicitly requested. This document holds the current execution design and setup
instructions; consequential design choices are discussed in the conversation before implementation.

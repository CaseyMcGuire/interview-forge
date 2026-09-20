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

Official admission selects the problem's ordered examples and hidden tests and saves their
inputs, expected answers, IDs, and visibility in a grading job alongside the submission.
`totalCases` is known while queued. No per-case result rows are created, and the former
custom-Run limit of twenty cases does not apply to official submissions.

Custom-run admission, owner polling, and result storage are reviewed and committed.
Stage 10 adds asynchronous reference preparation and shared grading and is reviewed and committed.
Expiration cleanup and frontend integration remain later stages.

## Grading-job design and review plan

Official admission creates the submission and a ready grading job in one transaction,
using the problem's stored inputs and expected answers. Custom admission saves only
the queued run and its cases. The background worker executes the reference solution, then
saves the generated expectations and creates the grading job together. Both paths use
the same `CodeGrader`; all compilation and execution stay outside API requests and database
transactions. No separate expected-output job table is needed.

A grading job holds the submitted source, ordered input/expected-output snapshots, the
problem-language reference, and exactly one originating attempt. Case metadata supports
retaining the first official failure or all custom results. Runtime selection stays in
application configuration. Saving the final result and deleting the job are atomic.

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
- [x] **8. Docker execution:** Implement the shared execution contract, collect every
  reachable case's output in one JVM, and update the protocol reader and current official
  caller together. Include lifecycle, output-limit, and Docker tests; rebuild the image.
- [x] **9. Official grading-job flow:** Create jobs during official admission, process
  them through the shared grader, and atomically retain the summary/first failure and
  delete the job. Replace the old official runner/scheduler path and test recovery.
- [x] **10. Custom reference preparation:** Claim queued custom runs, generate expected
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
Stage 9 connects official admission and execution to these jobs.

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
those classes or their old result wrappers.

Stage 8 implements `CodeExecutionService` in `DockerExecutionService` and connects the
existing official runner to `CodeGrader`. It is reviewed and committed. Docker
compiles once, then invokes all reachable inputs in one JVM without expected answers.
The driver reports each case's status, streams, and elapsed time. The application parses
strict JSON answers and grades them. Official persistence still retains only the first
failed case, including its own duration and outcome even when a later process failure
changes the overall verdict. Stage 9 replaces the official caller with job processing;
custom reference preparation remains stage 10. No schema or generated-artifact changes are required.

Stage 7 validation: `./gradlew test --tests '*CodeGraderTest' --tests '*JsonOutputCheckerTest'`
passed all 17 then-current unit tests using a fake execution service. Stage 8 adds
real Docker and database coverage; no generated artifacts changed.

Initial stage 8 validation passed all 62 then-current tests with this focused command:

```sh
./gradlew test \
  --tests com.application.execution.CodeGraderTest \
  --tests com.application.execution.JsonOutputCheckerTest \
  --tests com.application.execution.TestSuiteOutputReaderTest \
  --tests com.application.execution.SubmissionSchedulerTest \
  --tests com.application.execution.DockerExecutionServiceTest \
  --tests com.application.execution.SubmissionExecutionIntegrationTest
```

`./gradlew :kotlin-runtime:installDist`,
`docker build -t interview-forge-kotlin:2.4.20 runtime/kotlin`, and `git diff --check`
also passed. No browser or full-project test run was performed for this backend stage.

The sandbox API review replaces separate compilation/run/close calls with
`DockerCodeSandbox.runTests`. The service uses the concrete sandbox directly. Docker
failure scenarios each exercise a fresh sandbox; lifecycle checks cover invalid inputs,
creation without execution resources, and cleanup after partially staging source files.
All 43 Docker and submission integration tests passed after these changes:

```sh
./gradlew test \
  --tests com.application.execution.DockerExecutionServiceTest \
  --tests com.application.execution.SubmissionExecutionIntegrationTest
```

Stage 9 is reviewed and committed. Official admission writes the attempt and
its job atomically; claiming advances both together, and completion retains only the summary
and first failure while deleting the job. The scheduler calls the shared grader directly.
Tests cover stable snapshots after case deletion, current judge settings, claim/completion
rollback through EntKt hooks, recovery, polling privacy, and real Docker execution.
The GraphQL `totalCases` description now reflects the count selected at admission.
No database schema or dependency changes are required.

Stage 9 validation passed all 46 focused tests:

```sh
./gradlew test \
  --tests com.application.execution.GradingJobSchedulerTest \
  --tests com.application.execution.SubmissionExecutionIntegrationTest \
  --tests com.application.graphql.SubmissionIntegrationTest \
  --tests com.application.execution.GradingJobIntegrationTest
```

The rollback tests use EntKt hooks to fail job updates and deletion after submission
writes; they verify that the transaction preserves both records' previous states.

## Custom test suite admission and polling

`enqueueCustomTestSuiteRun` accepts a problem-language ID, source code, and ordered JSON inputs. Each
accepted request creates a new owned `CustomTestSuiteRun` and its `CustomTestCase` rows
in a serializable transaction. Invalid requests roll back the run and its cases together.
Source and stored case content use EntKt validation; request validation handles
the case count and decoding JSON. Expected outputs start absent and are prepared
asynchronously using the private reference solution on `JudgeConfiguration`.

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
part of this response. Integration tests exercise admission, asynchronous preparation,
grading, and owner polling together.

## Custom reference preparation

`CustomTestSuiteScheduler` prepares queued custom runs. `GradingScheduler` independently
processes ready grading jobs for both kinds of attempt. Scheduling uses two threads, and
each scheduler processes one item at a time, so reference execution and grading can overlap.
Both queues use creation time and ID order. Reference execution and submitted execution each
use a fresh sandbox that compiles once and runs its full list of inputs in one JVM.

1. Claim the oldest QUEUED custom run and mark it RUNNING with its start time.
2. Load its immutable ordered inputs and current reference settings through
   `CodeExecutionSettingsService`. Execute the reference solution through `CodeExecutionService`.
3. Require successful execution and a valid bounded JSON answer for every input. JSON null is
   a valid answer; missing answers, compilation errors, runtime failures, or oversized answers
   finish the run as REFERENCE_SOLUTION_FAILED, with all submitted cases NOT_RUN. Infrastructure
   and configuration exceptions finish it as INTERNAL_ERROR. Reference diagnostics are not retained.
4. Save all expected answers and a QUEUED grading job in one transaction. The job contains the
   user's source, inputs, and the generated answers. The custom run stays RUNNING while awaiting grading.
5. Claim the job through `GradingJobService`, execute the submitted code with `CodeGrader`, then
   atomically retain all case results and the summary and delete the job. The run's start time
   remains the start of preparation; runtimeMs measures only submitted execution.

`ExecutionStartup.workersReady()` completes Docker cleanup and database recovery before
either scheduler can claim work. Recovery is synchronized, retries on failure, and never runs
globally again once workers start. Interrupted RUNNING grading jobs finish their originating
attempt with INTERNAL_ERROR. RUNNING custom runs without a job are interrupted preparations;
a custom run with a QUEUED job continues to grading without executing the reference again.

After startup, each scheduler remembers only its own unfinished claim. If the final write
fails, the next tick recovers that ID before claiming more work. Missing jobs and already-finished
runs indicate that completion committed; custom runs with a grading job are prepared and must
not be failed. Neither scheduler scans the other scheduler's active work during normal operation.
Expiration does not cancel queued work; deletion remains stage 11.

Stage 10 is reviewed and committed. No database schema or dependency changes are required.
GraphQL timing descriptions now distinguish preparation from submitted execution; DGS and Relay
generation passed without changes to committed generated artifacts.
The official settings loader is now the shared `CodeExecutionSettingsService`,
and `CodeExecutionSettings` replaces the grading-only name. Separate `GradingScheduler` and
`CustomTestSuiteScheduler` classes share the startup gate but own their respective processing loops.

All 71 focused tests passed, covering real Docker reference-then-submission execution, privacy after problem
archival, mixed official/custom queue processing, transaction rollback through EntKt hooks,
and concurrent preparation/grading without recovering live work:

```sh
./gradlew test \
  --tests com.application.execution.CustomTestSuiteExecutionIntegrationTest \
  --tests com.application.execution.GradingSchedulerTest \
  --tests com.application.execution.CustomTestSuiteSchedulerTest \
  --tests com.application.execution.SubmissionExecutionIntegrationTest \
  --tests com.application.execution.CustomTestSuiteRunPersistenceIntegrationTest \
  --tests com.application.graphql.CustomTestSuiteRunIntegrationTest
```

After review refinements, all 8 scheduler tests and `buildRelay` passed. The final helper
extractions and explicit outcome mapping passed `compileTestKotlin` and `git diff --check`.
No full-project test suite or browser checks were run.

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

`GradingScheduler` coordinates claiming, grading, and finishing ready jobs.
`GradingJobService.claimNextQueuedGradingJob()` uses the status/creation-time index and a
row lock to claim the oldest ready job, breaking ties by ID. The job and its submission
become RUNNING together. Custom attempts already became RUNNING during preparation;
claiming their grading job preserves the original start time.

`CodeExecutionSettingsService.loadSubmittedCodeSettings()` reads the current judge and runtime settings.
`CodeGrader.gradeCode` receives the job's source and selected cases, uses `CodeExecutionService`
to compile and execute, and compares every returned answer. Compilation and execution happen
outside database transactions. The scheduler passes the result to `finishGradingJob`, which
routes to `SubmissionService.finishSubmission` or `CustomTestSuiteRunService.finishCustomTestSuiteRun`
and deletes the job in the same transaction.
The old `SubmissionRunner`, `SubmissionScheduler`, and submission-specific execution wrappers
are removed. Runtime, driver, checker, and limits remain current configuration; the selected
case snapshots persist only until the job finishes.

The summary and at most the first failed case are saved together in one transaction.
`Submission.failedTestResult` is an optional one-to-one edge to `SubmissionFailure`.
The `submission_failures.submission_id` unique constraint prevents retaining more than
one failure for an attempt. `Submission` represents
official attempts only and has no run/submit discriminator.
The submission service selects the first failing graded case and retains its original input,
expectation, and visibility. Later test edits cannot change that retained data.
All reachable cases run, so the passed count also includes successes after a failure.
A later process failure can determine the summary verdict while the retained case keeps
its own outcome.
Case positions remain on `TestCase` and are not copied into retained failures.
Passing outputs are not retained. Compilation failures and JVM failures before any case
starts have no failed-case row. Suite timing belongs to the summary; a failed case stores
its measured invocation duration. Abrupt process exits can leave that duration unknown.

Only the execution identity can create failed-case records. The authenticated submission
owner may read a failed example from a finished official submission; hidden and custom
records remain execution-only. The EntKt privacy rule uses the retained visibility, so a
hidden test made public later does not expose old failures. A retained public example stays
available to its owner after test edits or problem archival. Other users and administrators
cannot read it. Public errors exclude private diagnostics.
The existing owner polling API exposes RUNNING and the terminal summary; there are no
per-case progress writes during the suite.

Shared startup recovery cleans abandoned Docker resources and finishes leftover RUNNING
jobs before either scheduler starts. Finishing a recovered job also finishes its attempt and
deletes the job. This assumes one application instance, with one worker per queue. Execution
exceptions produce a safe infrastructure error; interruptions propagate with the thread's
interruption flag restored. A failed final write leaves the job ID with its owning scheduler;
its next tick checks that job without running the code again or touching other active attempts.

Both schedulers check their queues every second after startup, provided a configured runtime
is available. Each loop is sequential, but the two loops can run concurrently.
`execution.worker-enabled=false` pauses automatic processing; tests invoke schedulers directly.
Fake-execution tests cover failure handling, while real Docker tests exercise compilation and
grading through the submission/polling API.

Before switching from the old worker, drain any existing queued or running official
submissions: those older attempts have no grading jobs. This stage does not backfill them.

`executeCode` receives an execution ID such as `grading-job-123`. Docker attaches both an
ownership label and an execution label when creating compilation and suite containers. Recovery
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

`CodeExecutionService.executeCode` accepts a prepared program, ordered JSON inputs,
case time limit, and shared memory limit. `DockerExecutionService` manages the shared
`executionRoot` and its permissions, creates a `DockerCodeSandbox`, and calls `runTests`.
`DockerCodeSandbox` creates its temporary directory in `runTests`, then stages the
source files, compiles, and executes inside one `try/finally` that removes the directory.
Staging failures use that same cleanup path. Constructing a sandbox does not allocate
execution resources. Staging, compilation, and individual container operations are
private helpers; callers never need to compile separately or close the sandbox.
The result is `CodeExecutionResult`, distinguishing compilation failure from the suite's
process status and individual case results. `CodeGrader` owns comparison; expected
answers stay outside the container.

The Docker implementation runs one JVM for the whole suite, preserving state between
cases. Ordinary exceptions, invalid answers, and output overflows do not prevent later
invocations. Timeouts, heap exhaustion, or an abrupt JVM exit can stop the suite; completed
results remain available and the grader marks unreached cases `NOT_RUN`.
Containers run without application credentials or network access. The suite driver and
solution share a JVM, so the report protocol is not a tamper-proof grading boundary.

Only record available measurements. Preserve JSON numeric values during comparison;
object key order and whitespace do not matter, array order and JSON types do. Compilation,
runtime, time/memory limits, output errors, and infrastructure failures need explicit
outcomes, with safe diagnostics excluding hidden inputs.

`KotlinTestSuite` invokes the existing driver's top-level `main()` or `main(args)` for each
case in the same JVM and class loader. Only stdin/stdout/stderr are rebound between calls.
The driver continues to print its JSON answer to stdout. The application retains that
raw stream and parses it into `outputJson`; extra logging on stdout makes the answer invalid.
Driver invocations retain the configured per-case deadline and independent 20,000-byte
stdout/stderr limits. Measured case durations include driver loading and invocation but
exclude JVM startup. A watchdog halts a looping case; the container's outer deadline is
`min(caseCount * timeLimitMs + 2,000, 600,000)` milliseconds. An all-pass report also requires
a successful JVM exit, so leftover non-daemon threads can cause a timeout.

The Docker sandbox uses an unprivileged user, a read-only root, dropped capabilities,
one CPU, 128 processes, a 64 MiB temporary filesystem, and the configured shared memory
limit. Compilation has a separate 60-second/1 GiB budget. Source files are removed after
compilation, and the suite's program mount is read-only. A temporary read-only suite input
file supplies stdin and EOF. Container-side deadlines continue if the application stops.

The suite emits a case-start checkpoint and case-finished result for each invocation,
followed by one terminal status. The host reserves 256,000 bytes per case plus 1,024 bytes
for the terminal report, allowing both streams to expand when JSON-escaped. The reader
validates event order, completeness, byte limits, and nonnegative case durations. It
rejects malformed reports and preserves completed results before an abrupt JVM exit.
Checkpoints identify an invocation that started but could not report its result. All containers, temporary
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
`ProgramResult`, `TestSuiteProtocol`, and `JsonOutputChecker` live in the shared
`kotlin-runtime` module; execution and grading result models live in the application.
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

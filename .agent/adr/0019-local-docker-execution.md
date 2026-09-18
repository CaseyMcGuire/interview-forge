# 0019: Local Docker execution for official submissions

Date: 2026-09-16

Status: Worker and Docker runtime reviewed and committed. The result API
remains a separate review stage.

## Decision

Use one scheduled worker in the application and the Docker Engine API to run official
submissions. This is a single-instance, single-user application; no distributed worker
coordination, leases, or retry infrastructure is introduced. EntKt `forUpdate()` claims
queued submissions in a short transaction. Compilation and processes run outside database
transactions. Interrupted attempts finish with INTERNAL_ERROR and can be submitted again.

`ProgramExecutor` owns a disposable program workspace. `DockerExecutionService` provides
the implementation and runtime availability check; language execution configurations
continue to describe source files and commands. `execution.runtimes.kotlin` identifies a
prebuilt local image containing Kotlin 2.4.20, Java 21, and GNU timeout. Image downloads
and compiler installation happen only during an explicit image build.

Compile once, then run the ordered suite in one restricted container and one JVM. Disable
networking, drop capabilities, deny privilege escalation, use an unprivileged user,
and bound memory, CPU, processes, temporary storage, runtime, and captured output.
Only the disposable compile workspace is writable; suite execution mounts it read-only.
Pass inputs and expected outputs to the JVM. Compare strict JSON inside the container
using `JsonOutputChecker`, preserving numeric precision and distinguishing integers from
decimals. Docker's container-side timeout remains effective
if the application crashes; startup cleanup removes leftovers for its workspace.

Persist progress and the terminal summary, plus at most the first failed case in the
existing result table. Do not retain passing outputs or copy the whole suite into the
database. Expose a failed public example to its owner; hidden failure records remain
execution-only even if the original case is later edited. Compilation and infrastructure
errors use generic public messages rather than leaking private driver diagnostics.

## Consequences

The client choice and input handling are recorded in
[0020: Docker API client for container execution](0020-docker-api-client.md).
The shared JVM, driver contract, and first-failure protocol are recorded in
[0021: One JVM for each submission's test suite](0021-single-jvm-test-suites.md).

The application requires local Docker access and a separately built runtime image for
execution. Build the image with the
[runtime setup instructions](../../docs/submission-execution.md#local-runtime-setup).
The default Kotlin runtime mapping selects the locally built image; missing images make
submission admission unavailable.
Only one application instance may manage a given database/workspace. Multiple workers,
transient example/custom runs, and frontend integration remain separate work.

Suite duration includes JVM startup. Peak memory is not reported because it is not
measured. A kernel OOM or JVM heap exhaustion produces a memory-limit verdict.
Raw output is bounded and private for hidden cases. No physical schema migration is
needed; final results reuse the existing result table and lifecycle columns.

References: [Docker container restrictions](https://docs.docker.com/reference/cli/docker/container/run/),
[Kotlin compiler usage](https://kotlinlang.org/docs/command-line.html).

# 0020: Docker API client for container execution

Date: 2026-09-16

Status: Implemented, reviewed, and committed with the Docker runtime stage.

## Decision

Use `docker-java` 3.7.1 with its Apache HttpClient 5 transport for container execution.
Spring owns one `DockerClient`, injects it into `DockerExecutionService`, and closes it
at application shutdown. Use the client's standard Docker context and environment
configuration. The application no longer launches the Docker CLI or manages its pipes
and reader/writer threads.

Keep existing container restrictions, prebuilt local images, container-side deadlines,
workspace isolation, and cleanup. The API's create operation never pulls a missing image.
Attach stdout/stderr before starting a container and forcibly remove the container
when it finishes, exceeds a limit, or is interrupted. Compilation retains at most
20,000 bytes of each stream. Suite execution reserves a larger stdout budget for its
bounded JSON report, as described in [0021](0021-single-jvm-test-suites.md).
Wait for container exit as well as output completion; a program can close its streams
while continuing to run. Read execution state through the client's typed response.

Supply the suite input through a temporary read-only bind mount and redirect it to stdin
inside the container. The HttpClient 5 transport's attached stdin did not deliver EOF
in the `cat` integration test. A file gives the program ordinary EOF semantics without
custom socket handling. Pass command arguments separately through `exec "$@"`, so they
are never interpolated into shell code. Delete the input file after each execution;
restart cleanup removes abandoned inputs along with other owned workspaces. No queued
inputs or test-case snapshots are persisted.

## Consequences

The client and HTTP transport add dependencies, but remove `ProcessRunner`, CLI output
parsing, and the extra CLI process lifecycle. Docker still runs locally and the workspace
must be accessible to its daemon. The CLI remains useful for the explicit runtime image
build, but is not required by the application at execution time.

Transport connections have a two-second connect timeout and a 70-second response timeout
to allow a silent compiler its full 60-second budget. Execution waits retain their own
container deadline plus a two-second transport allowance. A stalled Docker management request
can therefore take longer to fail than the former ten-second CLI command limit.

Container removal still depends on a responsive Docker daemon. Container-side `timeout`
and startup cleanup remain necessary if the application or daemon disconnects.
Containers carry workspace ownership and submission ID labels from creation. Recovery
removes owned containers and temporary workspaces before finishing interrupted submission
rows. No container ID is persisted, avoiding a gap between container creation and saving
its identity in the database. A cleanup failure leaves recovery pending for the next poll.

References: [docker-java client setup](https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md),
[supported transports](https://github.com/docker-java/docker-java/blob/main/docs/transports.md),
[Docker Engine API](https://docs.docker.com/reference/api/engine/).

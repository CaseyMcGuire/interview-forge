# 0016: Language runtimes in application configuration

Date: 2026-09-15

Status: Historical. Submission snapshot behavior is superseded by
[the current submission execution design](../../docs/submission-execution.md#submission-execution-and-result-retention).

## Decision

Configure the active execution environment for each language in `execution.runtimes`,
keyed by `Language.key`. Every problem using that language uses the same runtime.
`ExecutionProperties` binds this map from Spring Boot application configuration.
Languages and judge configurations do not store runtime settings in the database.
Judge configurations contain the problem-specific test driver, checker, and limits.

The map starts empty because no execution environments have been set up yet. Problems
and starter code can be authored independently. Once a runtime exists, configure its
identifier in `application.yaml` or deployment configuration, for example:

```yaml
execution:
  runtimes:
    kotlin: kotlin-2.1
```

The identifier above is illustrative; it must match an actual execution environment.
Runtime identifiers are not shell commands. A config change applies to subsequent attempts
after the application restarts; runtime administration and per-problem overrides are not needed.

Submissions keep their immutable runtime snapshot. When execution is implemented, enqueueing
must require a configured runtime for the language and copy it to the submission, rather
than reading the current config later. This change does not implement the execution service.

## Migration

Remove the judge runtime column. Deployments with existing judge runtimes should move
the intended value for each language to application configuration before migrating.
Existing submission snapshots are unchanged.

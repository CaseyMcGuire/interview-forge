# Add problems through MCP

Interview Forge serves stateless Streamable HTTP at `http://localhost:8080/mcp`.
The tools create shared problems, configure their language and judge code, manage tests,
and verify solutions through the application's existing grading queue.

## Configure the application

Use an existing account with the `ADMIN` role. `MCP_USER_ID` is that account's numeric
`users.id`, not its email or a GraphQL global ID. Every request checks its current role.

Generate a token with `openssl rand -hex 32`, then add these values to the root `.env`:

```dotenv
MCP_API_TOKEN=your-generated-token
MCP_USER_ID=your-admin-user-id
```

Replace both placeholders. Keep the existing database settings. This project's `.env`
loader requires plain `KEY=value` lines without blank lines, comments, quotes, or `=`
inside values. The token must have at least 32 characters and contain no whitespace.

Restart with `./gradlew bootRun`. For an IDE launch, supply the same two environment
variables in its run configuration. MCP becomes available when both values are configured;
partial or invalid configuration fails startup. With neither configured, `/mcp` returns 404.
The server uses a bearer token; a browser login session or OAuth login does not authenticate MCP.

Catalog and authoring tools work without Docker. To generate answers and grade submissions,
start Docker and build the configured runtime image:

```sh
./gradlew :kotlin-runtime:installDist
docker build -t interview-forge-kotlin:2.4.20 runtime/kotlin
```

Leave `execution.worker-enabled=true` (the default) so queued submissions are processed.
See [execution setup](submission-execution.md#local-runtime-setup) for Docker configuration.

## Connect Codex

Add this entry to `~/.codex/config.toml`, or to `.codex/config.toml` for a trusted project:

```toml
[mcp_servers.interview-forge]
url = "http://localhost:8080/mcp"
bearer_token_env_var = "INTERVIEW_FORGE_MCP_TOKEN"
tool_timeout_sec = 180
```

The longer tool timeout accommodates compilation and reference execution. From the project
root, load the same token into the environment used to launch Codex without printing it:

```sh
export INTERVIEW_FORGE_MCP_TOKEN="$(sed -n 's/^MCP_API_TOKEN=//p' .env)"
codex
```

Use `/mcp` in the CLI to inspect the connection. Desktop/IDE clients need that environment
variable available to their Codex host; a variable exported in an unrelated terminal is
not automatically available to an already running app. These settings follow the
[official Codex MCP documentation](https://developers.openai.com/codex/mcp).

Any other client supporting Streamable HTTP can use the same URL and send
`Authorization: Bearer <your-generated-token>` on requests. The default Host/Origin
allowlists accept local connections. For a different hostname, configure `mcp.allowed-hosts`
and, if the client sends an Origin header, `mcp.allowed-origins` explicitly.

## Tools

| Tool | Purpose |
| --- | --- |
| `list_languages` | Read enabled language keys. |
| `search_problems` | Find published, unarchived problems before adding duplicates. |
| `get_problem` | Read the statement, language configuration, public examples, and grading cases. |
| `create_problem` | Publish a complete problem, language configurations, and tests atomically. |
| `update_problem` | Change title, statement, or difficulty; omitted fields stay unchanged. |
| `configure_problem_language` | Add or replace starter code, driver, reference solution, and limits together. |
| `add_test_cases` | Append an atomic batch of public examples and grading cases. |
| `update_test_case` | Replace a case's input, expectation, and explanation without changing visibility or position. |
| `generate_expected_output` | Run the stored reference for one input and return its answer without saving it. |
| `enqueue_problem_submission` | Queue supplied source, or omit source to verify the stored reference. |
| `get_problem_submission` | Poll the configured account's submission until `FINISHED`. |

Use slugs and language keys to identify content. Test-case and submission IDs are decimal
strings returned by these tools. JSON inputs and expectations are also strings: preserve
their number text, including large integers and decimal forms such as `1.0`. The string
`"null"` represents a JSON null answer; omitting an expectation does not.

Creation requires 1–20 language configurations and 1–20 public examples, with up to 100
additional grading cases. A batch addition accepts 1–100 cases total, including at most
20 public examples. Public examples are supplied in `publicExamples`; hidden grading cases
are supplied in `testCases`. These are per-request limits. Each JSON value must fit within
20,000 characters when compactly serialized.

Validation failures return MCP `isError: true` with an `errors` array containing field names
and messages. Successful creation publishes immediately. Batch additions are not deduplicated;
inspect `get_problem` before retrying a write whose response was lost. Expected outputs are
saved as supplied, so generating one does not automatically add or update a case.

## Authoring workflow

1. Call `list_languages` and `search_problems`; use `get_problem` to inspect an existing match.
2. Create the problem with complete starter, driver, and reference code. The runnable
   [echo-integer example](examples/echo-integer.json) is a `create_problem` argument object.
3. Call `generate_expected_output` for each additional input, then pass its returned
   `expectedOutputJson` to `add_test_cases`. Inspect generated answers against the statement;
   they establish what the reference does, not whether the reference is correct.
4. Call `enqueue_problem_submission` with the slug and language key, omitting `sourceCode`
   to verify the stored reference against every saved test.
5. Poll `get_problem_submission` with its returned ID about once per second until `FINISHED`.
   Expect `ACCEPTED` and matching `passedCases`/`totalCases`.
6. Enqueue a deliberately incorrect solution and confirm `WRONG_ANSWER`. Adjust weak tests
   or incorrect code using the update tools, then verify again.

For example, after creating the example problem, generate an answer with:

```json
{
  "slug": "mcp-echo-integer",
  "languageKey": "kotlin",
  "inputJson": "9007199254740993"
}
```

The result contains `expectedOutputJson: "9007199254740993"`. Save that case using
`add_test_cases` with `slug` and `testCases: [{"inputJson": "9007199254740993",
"expectedOutputJson": "9007199254740993"}]`. Enqueue reference verification with
`{"slug": "mcp-echo-integer", "languageKey": "kotlin"}`.

Official submission polling returns the summary and, when applicable, its first failed
public example. It does not expose hidden failure details or retain all passing outputs.
The default queue allows two active attempts per user and 100 across the application.

## Kotlin driver contract

The compiler receives `Solution.kt` (the submitted or reference code) and `TestDriver.kt`
(the stored driver). Both compile with the Kotlin/JDK standard libraries; additional JSON
libraries are not on the submission compiler's classpath.

The driver defines a top-level `main` in the default package without `@file:JvmName`.
For each case it reads one JSON value from stdin, calls the solution, and prints exactly
one JSON value to stdout. The runtime invokes the same driver repeatedly in one JVM, so
global state persists across cases. Use stderr for diagnostics. All language configurations
for a problem must implement the same JSON input/output contract.

## Verify the connection and workflow

```sh
./gradlew test --tests com.application.mcp.McpAuthoringWorkflowIntegrationTest
```

This test uses the official Java MCP client against a real HTTP server, an isolated
PostgreSQL database, the normal scheduled worker, and Docker. It loads the checked-in example,
generates and saves an answer, then verifies both passing and failing submissions. The
Docker portion requires the runtime image above; the test reports a skip if it is missing.

If the client cannot connect: 404 means MCP is unconfigured, 401 means the token is missing
or incorrect, 403 can mean the configured account is missing/not an administrator or the
Origin is rejected, and 421 means the Host is rejected. An execution-unavailable tool error
usually means the reference, supported runtime, or Docker image is unavailable. A submission
that stays queued requires an enabled worker and an available runtime; inspect the server log.

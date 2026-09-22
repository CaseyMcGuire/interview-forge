---
name: add-problem
description: Add coding problems to Interview Forge from a description, interview question, or source link using its MCP tools. Also use when adding test cases to an existing problem. Covers problem content, starter and reference code, generated expectations, and grading verification.
---

# Add a Problem

Create the requested problem through the connected Interview Forge MCP server. This is a content-authoring task; use the MCP tools instead of SQL, seed migrations, or application code changes.

## Connect and inspect

- Discover the Interview Forge tools in the current client. Tool names below are unprefixed; use the names exposed by the connection.
- Read [the MCP guide](../../../docs/mcp.md) for argument limits, execution contracts, and troubleshooting. Its [echo-integer payload](../../../docs/examples/echo-integer.json) is a complete `create_problem` example; adapt its structure to the requested problem.
- If the tools are unavailable, report the missing connection and point to the guide. Content preparation can continue, but do not claim the problem was saved or silently switch to direct database writes. Keep authentication tokens out of skill files and responses.
- Call `list_languages` and `search_problems`. Search uses case-sensitive title substrings, so check likely title variants and inspect plausible matches with `get_problem`. Reuse a matching problem when the request is to extend it; do not overwrite an existing problem as part of an unrelated addition.

## Prepare the problem

Read the supplied source when there is one. Preserve its actual rules and distinctive requirements rather than substituting a familiar LeetCode problem. If the source is inaccessible, ask for the missing statement. Clarify ambiguities that change correct answers; make reasonable minor choices and state those assumptions in the problem.

Prepare a self-contained Markdown statement with requirements, constraints, examples, and a precise input/output contract. Include a source link when adapting a linked question. Choose a stable slug, a descriptive title, and `EASY`, `MEDIUM`, or `HARD` difficulty.

For each requested language, prepare:

- Starter code containing the solution signature or stateful interface the user must implement.
- A complete reference solution, separate from the starter code.
- A test driver that parses input, calls that interface, and serializes the result.
- Execution limits appropriate to the stated constraints.

Use an enabled language with a supported runtime. Kotlin is the current implemented runtime; do not infer runtime support solely from `list_languages`. For Kotlin, follow the guide's driver contract: `Solution.kt` and `TestDriver.kt`, a top-level `main` in the default package, and Kotlin/JDK standard libraries only. The driver reads one JSON value and prints one JSON value per invocation. Use stderr for diagnostics. All cases run in one JVM, so do not assume global state resets between cases.

Make the expected answer deterministic, including ordering when multiple answers could otherwise be valid. If the problem requires a custom checker, identify that limitation: the current authoring tools do not configure checker source. Do not silently change the problem's correctness rules to fit exact comparison.

## Create and add tests

1. Independently work out at least one public example's expected answer. `generate_expected_output` needs a saved reference solution, so it cannot bootstrap an unsaved problem.
2. Call `create_problem` with the statement, complete language configurations, and public examples. Include independently verified grading cases if already prepared. Creation publishes immediately and saves the supplied content atomically; it does not compile or validate the reference solution's correctness.
3. Build further inputs that exercise the problem's branches and boundaries, including cases that distinguish plausible incorrect solutions. Use valid inputs within the statement's constraints.
4. Call `generate_expected_output` with the saved slug, language key, and each additional input. Inspect generated answers against independently reasoned examples; agreement with the reference alone does not prove correctness.
5. Save the returned `expectedOutputJson` with `add_test_cases`. Use `publicExamples` for cases shown in the problem statement and `testCases` for hidden grading cases. Generating an answer does not save a case.

When adding tests to an existing problem, start with `get_problem`, use its established JSON contract and reference solution, and proceed from input generation. Do not create another problem or replace its code unless requested.

Inputs and expectations are serialized JSON **strings**, not nested MCP argument objects. Preserve number text, including large integers and decimal forms such as `1.0`; do not round-trip them through JavaScript numbers. The string `"null"` is a JSON null answer, not an absent expectation. Copy generated expectations unchanged into the save call.

Inspect tool failures, including MCP `isError` and field errors, before continuing. After a lost response to `create_problem` or `add_test_cases`, inspect `get_problem` before retrying: additions are not deduplicated. Use `update_test_case` with the returned decimal case ID for corrections, preserving an existing explanation when appropriate; omitting it clears it. `configure_problem_language` replaces the complete configuration, so supply all intended code and limits when correcting one.

## Verify grading

- Call `enqueue_problem_submission` with the slug and language key, omitting `sourceCode` to grade the stored reference against every saved case.
- Poll `get_problem_submission` with its returned decimal ID about once per second until `FINISHED`. Require `ACCEPTED` and matching `passedCases` and `totalCases` to call reference verification successful. Verify each language configuration created.
- Submit a compiling, deliberately incorrect solution as `sourceCode` and confirm `WRONG_ANSWER`. This checks that the driver actually uses the submitted solution and that the cases detect an incorrect answer; a compilation failure does not establish that.
- Correct code or expectations through the relevant update tools and verify again. Do not weaken correct tests to accommodate a broken reference solution.
- Bound polling to five minutes per attempt. If it remains queued or running, report the submission ID and pending status rather than enqueueing duplicates. If Docker, the runtime, or the worker is unavailable, report which verification remains incomplete. Polling exposes only the summary and a failed public example when available, not hidden failure details.

Finish with the problem's `/problem/{slug}` URL on the connected application, languages configured, public and hidden case counts, and observed verification verdicts. Distinguish content saved from execution verified, and mention unresolved assumptions or blockers.

import type {
  CustomInputSubmissionCaseOutcome,
  TestPanel_submission$data
} from "__generated__/TestPanel_submission.graphql";

export type CustomInputDraft = {
  id: string;
  inputJson: string;
};

export type TestCaseTone = "idle" | "running" | "passed" | "failed";
export type TestCaseResult = NonNullable<TestPanel_submission$data["caseResults"]>[number];

export type TestCaseRowData = {
  draft: CustomInputDraft;
  index: number;
  result: TestCaseResult | null;
  tone: TestCaseTone;
  resultText: string | null;
};

const caseOutcomeLabels: Partial<Record<CustomInputSubmissionCaseOutcome, string>> = {
  PASSED: "Passed",
  WRONG_ANSWER: "Wrong answer",
  INVALID_OUTPUT: "Invalid output",
  RUNTIME_ERROR: "Runtime error",
  TIME_LIMIT_EXCEEDED: "Time limit exceeded",
  MEMORY_LIMIT_EXCEEDED: "Memory limit exceeded",
  OUTPUT_LIMIT_EXCEEDED: "Output limit exceeded",
  INTERNAL_ERROR: "Unable to run",
  NOT_RUN: "Not run"
};

export function createTestCaseRow(
  draft: CustomInputDraft,
  index: number,
  results: readonly TestCaseResult[] | null,
  isPending: boolean
): TestCaseRowData {
  // A result belongs to a row only while the row still holds the input that was run.
  const candidate = results?.[index] ?? null;
  const result = candidate && candidate.testCase.inputJson === draft.inputJson ? candidate : null;

  if (isPending) {
    return {draft, index, result: null, tone: "running", resultText: "Running…"};
  }

  if (!result) {
    return {draft, index, result: null, tone: "idle", resultText: null};
  }

  if (result.outcome === "PASSED") {
    return {draft, index, result, tone: "passed", resultText: result.output.length > 0 ? result.output : "No output"};
  }

  if (result.outcome === "NOT_RUN") {
    return {draft, index, result, tone: "idle", resultText: "Not run"};
  }

  return {draft, index, result, tone: "failed", resultText: caseOutcomeLabels[result.outcome] ?? "Failed"};
}

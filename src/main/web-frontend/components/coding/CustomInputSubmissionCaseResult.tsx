import * as stylex from "@stylexjs/stylex";
import {graphql, useFragment} from "react-relay";
import type {
  CustomInputSubmissionCaseOutcome,
  CustomInputSubmissionCaseResult_result$key
} from "__generated__/CustomInputSubmissionCaseResult_result.graphql";

type Props = {
  result: CustomInputSubmissionCaseResult_result$key;
};

const outcomeLabels: Partial<Record<CustomInputSubmissionCaseOutcome, string>> = {
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

const styles = stylex.create({
  case: {
    border: "1px solid #43454a",
    borderRadius: 6,
    minWidth: 0
  },
  summary: {
    padding: "10px 12px",
    fontSize: 12,
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  outcome: {
    marginLeft: 12,
    color: "#9da0a8"
  },
  passed: {
    color: "#89cc8e"
  },
  failed: {
    color: "#f2a6a6"
  },
  body: {
    padding: "0 12px 12px"
  },
  error: {
    margin: "0 0 10px",
    fontSize: 12,
    lineHeight: 1.6,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere",
    color: "#f2a6a6"
  },
  values: {
    display: "grid",
    gridTemplateColumns: {
      default: "repeat(3, minmax(0, 1fr))",
      "@media (max-width: 1100px)": "minmax(0, 1fr)"
    },
    gap: 10,
    margin: 0
  },
  block: {
    minWidth: 0,
    padding: "9px 12px",
    border: "1px solid #393b40",
    borderRadius: 5,
    backgroundColor: "#1e1f22"
  },
  label: {
    marginBottom: 4,
    color: "#9da0a8",
    fontSize: 11,
    fontWeight: 600
  },
  value: {
    margin: 0
  },
  output: {
    margin: 0,
    color: "#bcbec4",
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    lineHeight: 1.7,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  unavailable: {
    color: "#9da0a8",
    fontSize: 12
  }
});

export default function CustomInputSubmissionCaseResult(props: Props) {
  const result = useFragment(graphql`
    fragment CustomInputSubmissionCaseResult_result on CustomInputSubmissionCaseResult {
      outcome
      output
      publicErrorMessage
      testCase {
        position
        inputJson
        expectedOutputJson
      }
    }
  `, props.result);

  const {testCase} = result;
  const passed = result.outcome === "PASSED";
  const notRun = result.outcome === "NOT_RUN";

  // Render the serialized values verbatim: parsing JSON here would hide 1 versus 1.0 and round large integers.
  return (
    <details sx={styles.case} open={!passed}>
      <summary sx={styles.summary}>
        Test {testCase.position + 1}
        <span sx={[styles.outcome, passed && styles.passed, !passed && !notRun && styles.failed]}>
          {outcomeLabels[result.outcome] ?? "Unknown result"}
        </span>
      </summary>

      <div sx={styles.body}>
        {result.publicErrorMessage && <p sx={styles.error}>{result.publicErrorMessage}</p>}

        <dl sx={styles.values}>
          <div sx={styles.block}>
            <dt sx={styles.label}>Input</dt>
            <dd sx={styles.value}><pre sx={styles.output}>{testCase.inputJson}</pre></dd>
          </div>
          <div sx={styles.block}>
            <dt sx={styles.label}>Expected output</dt>
            <dd sx={styles.value}>
              {testCase.expectedOutputJson !== null ? (
                <pre sx={styles.output}>{testCase.expectedOutputJson}</pre>
              ) : (
                <span sx={styles.unavailable}>Not available</span>
              )}
            </dd>
          </div>
          <div sx={styles.block}>
            <dt sx={styles.label}>Your output</dt>
            <dd sx={styles.value}>
              {result.output.length > 0 ? (
                <pre sx={styles.output}>{result.output}</pre>
              ) : (
                <span sx={styles.unavailable}>{notRun ? "Not run" : "No output"}</span>
              )}
            </dd>
          </div>
        </dl>
      </div>
    </details>
  );
}

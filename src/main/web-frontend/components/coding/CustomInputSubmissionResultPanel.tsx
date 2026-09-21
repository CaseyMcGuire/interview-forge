import * as stylex from "@stylexjs/stylex";
import {useId} from "react";
import {graphql, useFragment} from "react-relay";
import type {
  CustomInputSubmissionOutcome,
  CustomInputSubmissionResultPanel_submission$data,
  CustomInputSubmissionResultPanel_submission$key
} from "__generated__/CustomInputSubmissionResultPanel_submission.graphql";
import CustomInputSubmissionCaseResult from "./CustomInputSubmissionCaseResult";
import Control from "./WorkspaceControl";

type Props = {
  submission: CustomInputSubmissionResultPanel_submission$key | null;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
};

const outcomeSummaries: Partial<Record<CustomInputSubmissionOutcome, {label: string; message: string}>> = {
  PASSED: {
    label: "All tests passed",
    message: "Your code passed every supplied test."
  },
  WRONG_ANSWER: {
    label: "Wrong answer",
    message: "Some outputs did not match the expected results."
  },
  INVALID_OUTPUT: {
    label: "Invalid output",
    message: "Your program returned output that was not valid JSON."
  },
  COMPILE_ERROR: {
    label: "Compile error",
    message: "Your code did not compile."
  },
  RUNTIME_ERROR: {
    label: "Runtime error",
    message: "Your program stopped with an error."
  },
  TIME_LIMIT_EXCEEDED: {
    label: "Time limit exceeded",
    message: "Your program exceeded the time limit."
  },
  MEMORY_LIMIT_EXCEEDED: {
    label: "Memory limit exceeded",
    message: "Your program exceeded the memory limit."
  },
  OUTPUT_LIMIT_EXCEEDED: {
    label: "Output limit exceeded",
    message: "Your program produced too much output."
  },
  REFERENCE_SOLUTION_FAILED: {
    label: "Expected outputs unavailable",
    message: "Expected outputs could not be calculated for these inputs."
  },
  INTERNAL_ERROR: {
    label: "Unable to run tests",
    message: "We couldn't complete your tests. Please try again."
  }
};

const styles = stylex.create({
  panel: {
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column",
    color: "#bcbec4"
  },
  heading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    flexWrap: "wrap",
    gap: "8px 12px",
    padding: "12px 22px",
    borderBottom: "1px solid #393b40"
  },
  title: {
    margin: 0,
    fontSize: 12,
    fontWeight: 600
  },
  badge: {
    padding: "2px 7px",
    borderRadius: 4,
    fontSize: 11,
    backgroundColor: "#393b40",
    color: "#bcbec4"
  },
  pending: {
    backgroundColor: "#2e436e",
    color: "#b5ceff"
  },
  passed: {
    backgroundColor: "#294436",
    color: "#89cc8e"
  },
  failed: {
    backgroundColor: "#4b3034",
    color: "#f2a6a6"
  },
  body: {
    padding: "16px 22px",
    overflowY: "auto",
    scrollbarWidth: "thin",
    scrollbarColor: "#4e5157 transparent"
  },
  message: {
    margin: 0,
    fontSize: 12,
    lineHeight: 1.6,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  statistics: {
    display: "flex",
    flexWrap: "wrap",
    gap: "8px 24px",
    margin: "12px 0 0",
    fontSize: 12
  },
  statistic: {
    display: "flex",
    gap: 6
  },
  statisticLabel: {
    color: "#9da0a8"
  },
  statisticValue: {
    margin: 0,
    fontVariantNumeric: "tabular-nums"
  },
  error: {
    marginTop: 12,
    color: "#f2a6a6",
    fontSize: 12,
    overflowWrap: "anywhere"
  },
  retry: {
    width: "fit-content",
    marginTop: 8,
    padding: "5px 10px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    color: "#dfe1e5",
    fontSize: 12,
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  cases: {
    display: "flex",
    flexDirection: "column",
    gap: 10,
    marginTop: 16
  }
});

function describeSubmission(
  submission: CustomInputSubmissionResultPanel_submission$data | null | undefined,
  isLoading: boolean,
  hasError: boolean
) {
  if (!submission && isLoading) {
    return {label: "Loading", message: "Loading your test results…"};
  }

  if (!submission && hasError) {
    return {label: "Results unavailable", message: "Your test results could not be loaded."};
  }

  if (!submission) {
    return {label: "Not run", message: "Run your code against your inputs to see results for each test."};
  }

  switch (submission.status) {
    case "QUEUED":
      return {label: "Queued", message: "Your tests are waiting to run."};
    case "RUNNING":
      return {label: "Running", message: "Preparing expected outputs and testing your code."};
    case "FINISHED": {
      const summary = submission.outcome ? outcomeSummaries[submission.outcome] : undefined;

      return {
        label: summary?.label ?? "Finished",
        message: submission.publicErrorMessage ?? summary?.message ?? "No overall result is available."
      };
    }
    default:
      return {label: "Unknown status", message: "The status of these tests is unavailable."};
  }
}

export default function CustomInputSubmissionResultPanel(props: Props) {
  const {isLoading = false, error, onRetry} = props;
  const submission = useFragment(graphql`
    fragment CustomInputSubmissionResultPanel_submission on CustomInputSubmission {
      status
      outcome
      totalCases
      passedCases
      runtimeMs
      publicErrorMessage
      caseResults {
        testCase {
          id
        }
        ...CustomInputSubmissionCaseResult_result
      }
    }
  `, props.submission);

  const headingId = useId();
  const summary = describeSubmission(submission, isLoading, Boolean(error));
  const isPending = submission?.status === "QUEUED" || submission?.status === "RUNNING";
  const isFinished = submission?.status === "FINISHED";
  const allPassed = submission?.outcome === "PASSED";

  return (
    <section sx={styles.panel} aria-labelledby={headingId}>
      <div sx={styles.heading}>
        <h2 id={headingId} sx={styles.title}>Test results</h2>
        <span role="status" sx={[
          styles.badge,
          isPending && styles.pending,
          isFinished && allPassed && styles.passed,
          isFinished && !allPassed && styles.failed
        ]}>
          {summary.label}
        </span>
      </div>

      <div sx={styles.body}>
        <p sx={styles.message}>{summary.message}</p>

        {isFinished && (
          <dl sx={styles.statistics}>
            <div sx={styles.statistic}>
              <dt sx={styles.statisticLabel}>Passed</dt>
              <dd sx={styles.statisticValue}>{submission.passedCases} / {submission.totalCases} tests</dd>
            </div>
            {submission.runtimeMs !== null && (
              <div sx={styles.statistic}>
                <dt sx={styles.statisticLabel}>Code runtime</dt>
                <dd sx={styles.statisticValue}>{submission.runtimeMs} ms</dd>
              </div>
            )}
          </dl>
        )}

        {error && (
          <div sx={styles.error}>
            <div role="alert">{error}</div>
            {onRetry && (
              <Control
                appearance={[styles.retry, isLoading && styles.disabled]}
                disabled={isLoading}
                onActivate={onRetry}
              >
                {isLoading ? "Checking results…" : "Retry loading results"}
              </Control>
            )}
          </div>
        )}

        {isFinished && (
          <div sx={styles.cases}>
            {submission.caseResults?.map(result => (
              <CustomInputSubmissionCaseResult key={result.testCase.id} result={result} />
            ))}
            {submission.caseResults === null && <p sx={styles.message}>Case results are unavailable.</p>}
          </div>
        )}
      </div>
    </section>
  );
}

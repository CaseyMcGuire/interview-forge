import * as stylex from "@stylexjs/stylex";
import {useId} from "react";
import {graphql, useFragment} from "react-relay";
import type {
  ProblemSubmissionResultPanel_problemSubmission$data,
  ProblemSubmissionResultPanel_problemSubmission$key,
  ProblemSubmissionVerdict
} from "__generated__/ProblemSubmissionResultPanel_problemSubmission.graphql";
import ProblemSubmissionFailedExample from "./ProblemSubmissionFailedExample";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type Props = {
  problemSubmission: ProblemSubmissionResultPanel_problemSubmission$key | null;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
};

const verdictSummaries: Partial<Record<ProblemSubmissionVerdict, {label: string; message: string}>> = {
  ACCEPTED: {
    label: "Accepted",
    message: "All test cases passed."
  },
  WRONG_ANSWER: {
    label: "Wrong answer",
    message: "Your output did not match the expected result."
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
  INTERNAL_ERROR: {
    label: "Unable to judge",
    message: "We couldn't complete your submission. Please try submitting again."
  }
};

const styles = stylex.create({
  panel: {
    borderTop: "1px solid #43454a",
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column"
  },
  heading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    flexWrap: "wrap",
    gap: "8px 12px",
    minHeight: 45,
    padding: "8px 22px",
    borderBottom: "1px solid #393b40",
    flexShrink: 0
  },
  title: {
    display: "flex",
    alignItems: "center",
    gap: 9,
    margin: 0,
    fontSize: 12,
    fontWeight: 600
  },
  badge: {
    fontSize: 11,
    padding: "2px 7px",
    borderRadius: 4,
    backgroundColor: "#393b40",
    color: "#bcbec4"
  },
  pending: {
    backgroundColor: "#2e436e",
    color: "#b5ceff"
  },
  accepted: {
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
    color: "#bcbec4",
    fontSize: 12,
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
  }
});

function describeProblemSubmission(
  problemSubmission: ProblemSubmissionResultPanel_problemSubmission$data | null | undefined,
  isLoading: boolean,
  hasError: boolean
) {
  if (!problemSubmission && isLoading) {
    return {label: "Loading", message: "Loading your submission…"};
  }

  if (!problemSubmission && hasError) {
    return {label: "Status unavailable", message: "Your submission's status could not be loaded."};
  }

  if (!problemSubmission) {
    return {label: "Not submitted", message: "Submit your solution to check it against all test cases."};
  }

  if (problemSubmission.status === "QUEUED") {
    return {label: "Queued", message: "Your solution is waiting to be judged."};
  }

  if (problemSubmission.status === "RUNNING") {
    return {label: "Running", message: "Your solution is being checked against the test cases."};
  }

  if (problemSubmission.status === "FINISHED") {
    const summary = verdictSummaries[problemSubmission.verdict];

    return {
      label: summary?.label ?? "Finished",
      message: problemSubmission.publicErrorMessage ?? summary?.message ?? "No verdict is available for this submission."
    };
  }

  return {label: "Unknown status", message: "This submission's status is unavailable."};
}

export default function ProblemSubmissionResultPanel(props: Props) {
  const {isLoading = false, error, onRetry} = props;
  const problemSubmission = useFragment(graphql`
    fragment ProblemSubmissionResultPanel_problemSubmission on ProblemSubmission {
      status
      verdict
      totalCases
      passedCases
      runtimeMs
      publicErrorMessage
      failedExample {
        ...ProblemSubmissionFailedExample_example
      }
    }
  `, props.problemSubmission);

  const headingId = useId();
  const summary = describeProblemSubmission(problemSubmission, isLoading, Boolean(error));
  const isPending = problemSubmission?.status === "QUEUED" || problemSubmission?.status === "RUNNING";
  const isFinished = problemSubmission?.status === "FINISHED";
  const isAccepted = problemSubmission?.verdict === "ACCEPTED";

  return (
    <section sx={styles.panel} aria-labelledby={headingId}>
      <div sx={styles.heading}>
        <h2 id={headingId} sx={styles.title}>
          <Icon name="terminal" /> Submission
        </h2>
        <span role="status" sx={[
          styles.badge,
          isPending && styles.pending,
          isFinished && isAccepted && styles.accepted,
          isFinished && !isAccepted && styles.failed
        ]}>
          {summary.label}
        </span>
      </div>

      <div sx={styles.body}>
        <p sx={styles.message}>{summary.message}</p>

        {isFinished && (
          <dl sx={styles.statistics}>
            {problemSubmission.totalCases > 0 && (
              <div sx={styles.statistic}>
                <dt sx={styles.statisticLabel}>Passed</dt>
                <dd sx={styles.statisticValue}>{problemSubmission.passedCases} / {problemSubmission.totalCases} cases</dd>
              </div>
            )}
            {problemSubmission.runtimeMs != null && (
              <div sx={styles.statistic}>
                <dt sx={styles.statisticLabel}>Runtime</dt>
                <dd sx={styles.statisticValue}>{problemSubmission.runtimeMs} ms</dd>
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
                {isLoading ? "Checking status…" : "Retry status check"}
              </Control>
            )}
          </div>
        )}

        {isFinished && problemSubmission.failedExample && (
          <ProblemSubmissionFailedExample example={problemSubmission.failedExample} />
        )}
      </div>
    </section>
  );
}

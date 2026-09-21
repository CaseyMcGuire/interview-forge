import * as stylex from "@stylexjs/stylex";
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

/** Short verdict wording shared with the workspace strip. */
export function verdictLabel(verdict: ProblemSubmissionVerdict): string {
  return verdictSummaries[verdict]?.label ?? "Finished";
}

const styles = stylex.create({
  panel: {
    display: "flex",
    flexDirection: "column",
    gap: 12,
    padding: "14px 16px 16px"
  },
  verdictRow: {
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap",
    gap: "6px 10px"
  },
  verdict: {
    fontSize: 14,
    fontWeight: 600,
    color: "#dfe1e5"
  },
  pending: {
    color: "#b5ceff"
  },
  accepted: {
    color: "#89cc8e"
  },
  failed: {
    color: "#f2a6a6"
  },
  statistics: {
    display: "flex",
    flexWrap: "wrap",
    gap: "4px 14px",
    fontSize: 12,
    color: "#9da0a8",
    fontVariantNumeric: "tabular-nums"
  },
  message: {
    margin: 0,
    fontSize: 12.5,
    lineHeight: 1.5,
    color: "#bcbec4",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  error: {
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

  const summary = describeProblemSubmission(problemSubmission, isLoading, Boolean(error));
  const isPending = problemSubmission?.status === "QUEUED" || problemSubmission?.status === "RUNNING";
  const isFinished = problemSubmission?.status === "FINISHED";
  const isAccepted = problemSubmission?.verdict === "ACCEPTED";

  return (
    <section sx={styles.panel} aria-label="Submission">
      <div sx={styles.verdictRow}>
        {isFinished && <Icon name={isAccepted ? "passed" : "failed"} size={18} />}
        <span role="status" sx={[
          styles.verdict,
          isPending && styles.pending,
          isFinished && isAccepted && styles.accepted,
          isFinished && !isAccepted && styles.failed
        ]}>
          {summary.label}
        </span>
        {isFinished && (
          <span sx={styles.statistics}>
            {problemSubmission.totalCases > 0 && (
              <span>{problemSubmission.passedCases} of {problemSubmission.totalCases} hidden cases passed</span>
            )}
            {problemSubmission.runtimeMs != null && <span>{problemSubmission.runtimeMs} ms</span>}
          </span>
        )}
      </div>

      <p sx={styles.message}>{summary.message}</p>

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
    </section>
  );
}

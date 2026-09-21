import * as stylex from "@stylexjs/stylex";
import type {
  CustomInputSubmissionOutcome,
  TestPanel_submission$data
} from "__generated__/TestPanel_submission.graphql";
import AddTestButton from "./AddTestButton";
import TestStatusDot from "./TestStatusDot";
import type {TestCaseRowData} from "./testCaseRows";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

export type WorkspacePanel = "tests" | "submission";

export type SubmissionChip = {
  label: string;
  tone: "none" | "pending" | "passed" | "failed";
};

type Props = {
  panel: WorkspacePanel;
  onSelectPanel: (panel: WorkspacePanel) => void;
  expanded: boolean;
  onToggleExpanded: () => void;
  bodyId: string;
  rows: readonly TestCaseRowData[];
  maxCases: number;
  addDisabled: boolean;
  onAddCase: () => void;
  submission: TestPanel_submission$data | null | undefined;
  submissionChip: SubmissionChip;
  isLoading: boolean;
  hasError: boolean;
};

const outcomeLabels: Partial<Record<CustomInputSubmissionOutcome, string>> = {
  INVALID_OUTPUT: "Invalid output",
  COMPILE_ERROR: "Compile error",
  RUNTIME_ERROR: "Runtime error",
  TIME_LIMIT_EXCEEDED: "Time limit exceeded",
  MEMORY_LIMIT_EXCEEDED: "Memory limit exceeded",
  OUTPUT_LIMIT_EXCEEDED: "Output limit exceeded",
  REFERENCE_SOLUTION_FAILED: "Expected outputs unavailable",
  INTERNAL_ERROR: "Unable to run tests"
};

const styles = stylex.create({
  header: {
    height: 36,
    flexShrink: 0,
    display: "flex",
    alignItems: "center",
    gap: 8,
    padding: "0 6px 0 10px"
  },
  segmented: {
    display: "flex",
    gap: 2,
    padding: 2,
    borderRadius: 7,
    backgroundColor: "#26272b"
  },
  segment: {
    display: "inline-flex",
    alignItems: "center",
    gap: 7,
    height: 24,
    padding: "0 10px",
    borderRadius: 5,
    color: {
      default: "#9da0a8",
      ":hover": "#dfe1e5"
    },
    fontSize: 12,
    fontWeight: 600,
    whiteSpace: "nowrap",
    cursor: "pointer",
    userSelect: "none",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 1
  },
  segmentSelected: {
    backgroundColor: "#2e436e",
    color: {
      default: "#b5ceff",
      ":hover": "#b5ceff"
    }
  },
  segmentCount: {
    fontSize: 11,
    fontWeight: 500,
    opacity: 0.85
  },
  summary: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    minWidth: 0,
    marginLeft: 6,
    fontSize: 12
  },
  dots: {
    display: "flex",
    gap: 5
  },
  summaryText: {
    color: "#9da0a8",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis"
  },
  summaryStrong: {
    color: "#bcbec4"
  },
  spacer: {
    flexGrow: 1
  },
  iconButton: {
    display: "grid",
    placeItems: "center",
    width: 28,
    height: 28,
    flexShrink: 0,
    borderRadius: 6,
    color: {
      default: "#9da0a8",
      ":hover": "#dfe1e5"
    },
    cursor: "pointer",
    userSelect: "none",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 1
  }
});

function describeRun(
  submission: TestPanel_submission$data | null | undefined,
  isLoading: boolean,
  hasError: boolean
): {text: string; strong: boolean} {
  if (!submission && isLoading) {
    return {text: "Loading results…", strong: false};
  }

  if (!submission && hasError) {
    return {text: "Results unavailable", strong: false};
  }

  if (!submission) {
    return {text: "Not run yet", strong: false};
  }

  switch (submission.status) {
    case "QUEUED":
      return {text: "Queued…", strong: false};
    case "RUNNING":
      return {text: "Running…", strong: false};
    case "FINISHED": {
      const parts = [];
      const outcomeLabel = submission.outcome ? outcomeLabels[submission.outcome] : undefined;

      if (outcomeLabel) {
        parts.push(outcomeLabel);
      }

      parts.push(`${submission.passedCases} of ${submission.totalCases} passed`);

      if (submission.runtimeMs !== null) {
        parts.push(`${submission.runtimeMs} ms`);
      }

      return {text: parts.join(" · "), strong: true};
    }
    default:
      return {text: "Status unavailable", strong: false};
  }
}

export default function TestPanelHeader(props: Props) {
  const {panel, expanded, rows, submissionChip} = props;
  const run = describeRun(props.submission, props.isLoading, props.hasError);

  function renderPanelActions() {
    switch (panel) {
      case "tests":
        return (
          <>
            <div sx={styles.summary}>
              <span sx={styles.dots} aria-hidden="true">
                {rows.map(row => <TestStatusDot key={row.draft.id} tone={row.tone} />)}
              </span>
              <span role="status" sx={[styles.summaryText, run.strong && styles.summaryStrong]}>{run.text}</span>
            </div>
            <span sx={styles.spacer} />
            <AddTestButton
              disabled={props.addDisabled}
              limitReached={rows.length >= props.maxCases}
              maxCases={props.maxCases}
              onAdd={props.onAddCase}
            />
          </>
        );

      case "submission":
        return <span sx={styles.spacer} />;
    }
  }

  return (
    <div sx={styles.header}>
      <div sx={styles.segmented} role="group" aria-label="Workspace panels">
        <Control
          appearance={[styles.segment, panel === "tests" && styles.segmentSelected]}
          pressed={panel === "tests"}
          onActivate={() => props.onSelectPanel("tests")}
        >
          Tests <span sx={styles.segmentCount}>{rows.length}</span>
        </Control>
        <Control
          appearance={[styles.segment, panel === "submission" && styles.segmentSelected]}
          pressed={panel === "submission"}
          onActivate={() => props.onSelectPanel("submission")}
        >
          {submissionChip.tone !== "none" && (
            <TestStatusDot tone={submissionChip.tone === "pending" ? "running" : submissionChip.tone} />
          )}
          {submissionChip.label}
        </Control>
      </div>
      {renderPanelActions()}
      <Control
        appearance={styles.iconButton}
        label={expanded ? "Collapse panel" : "Expand panel"}
        expanded={expanded}
        controls={expanded ? props.bodyId : undefined}
        onActivate={props.onToggleExpanded}
      >
        <Icon name={expanded ? "chevronDown" : "chevronUp"} />
      </Control>
    </div>
  );
}

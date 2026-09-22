import * as stylex from "@stylexjs/stylex";
import {Link} from "react-router";
import {AppRoutes} from "routes/AppRoutes";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type Props = {
  title: string;
  editProblemUrl: string | null;
  problemOpen: boolean;
  onToggleProblem: () => void;
  editorAvailable: boolean;
  fontSize: number;
  onCycleFontSize: () => void;
  wordWrap: boolean;
  onToggleWordWrap: () => void;
  onReformat: () => void;
  resetDisabled: boolean;
  onReset: () => void;
  onRunTests: () => void;
  runDisabled: boolean;
  isEnqueuingTests: boolean;
  areTestsPending: boolean;
  onSubmit: () => void;
  submitDisabled: boolean;
  isSubmitting: boolean;
  isPending: boolean;
};

const styles = stylex.create({
  bar: {
    height: 44,
    flexShrink: 0,
    display: "flex",
    alignItems: "center",
    gap: 12,
    padding: {
      default: "0 12px 0 14px",
      "@media (max-width: 600px)": "0 8px"
    },
    backgroundColor: "#1e1f22",
    borderBottomWidth: 1,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40"
  },
  brand: {
    display: "flex",
    alignItems: "center",
    gap: 9,
    fontWeight: 650,
    fontSize: 13,
    letterSpacing: "-0.2px",
    color: "#dfe1e5",
    textDecoration: "none"
  },
  brandName: {
    display: {
      default: "inline",
      "@media (max-width: 900px)": "none"
    }
  },
  brandMark: {
    display: "grid",
    placeItems: "center",
    width: 24,
    height: 24,
    borderRadius: 6,
    backgroundColor: "#3574f0",
    color: "#ffffff"
  },
  divider: {
    width: 1,
    height: 18,
    flexShrink: 0,
    backgroundColor: "#393b40"
  },
  toolDivider: {
    margin: "0 8px"
  },
  title: {
    minWidth: 0,
    fontSize: 14,
    fontWeight: 600,
    color: "#dfe1e5",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis"
  },
  spacer: {
    flexGrow: 1
  },
  tools: {
    display: "flex",
    alignItems: "center",
    gap: 4
  },
  toolButton: {
    display: "grid",
    placeItems: "center",
    minWidth: 32,
    height: 32,
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
    outlineOffset: 2
  },
  toolPressed: {
    backgroundColor: "#2b2d30",
    color: "#dfe1e5"
  },
  toolDisabled: {
    opacity: 0.35,
    cursor: "default"
  },
  fontButton: {
    padding: "0 9px",
    fontSize: 12
  },
  action: {
    display: "inline-flex",
    alignItems: "center",
    gap: 7,
    height: 32,
    padding: "0 14px 0 11px",
    borderRadius: 6,
    borderWidth: 1,
    borderStyle: "solid",
    fontSize: 13,
    fontWeight: 600,
    cursor: "pointer",
    userSelect: "none",
    whiteSpace: "nowrap",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 3
  },
  runButton: {
    borderColor: "#4e5157",
    backgroundColor: "#2b2d30",
    color: "#dfe1e5"
  },
  editLink: {
    padding: "0 10px",
    textDecoration: "none",
    flexShrink: 0
  },
  submitButton: {
    marginLeft: 4,
    borderColor: "#3574f0",
    backgroundColor: "#3574f0",
    color: "#ffffff"
  },
  actionDisabled: {
    opacity: 0.6,
    cursor: "not-allowed"
  }
});

/** Single top bar: brand, problem identity, editor tools, and the run and submit actions. */
export default function WorkspaceToolbar(props: Props) {
  const {title, editProblemUrl, problemOpen, onToggleProblem, editorAvailable} = props;
  const {fontSize, onCycleFontSize, wordWrap, onToggleWordWrap, resetDisabled, onReset} = props;
  const {onRunTests, runDisabled, isEnqueuingTests, areTestsPending} = props;
  const {onSubmit, submitDisabled, isSubmitting, isPending} = props;
  const runLabel = isEnqueuingTests ? "Queueing…" : areTestsPending ? "Running…" : "Run";
  const submitLabel = isSubmitting ? "Submitting…" : isPending ? "Judging…" : "Submit";

  return (
    <div sx={styles.bar}>
      <Link {...stylex.props(styles.brand)} to={AppRoutes.Problems()} aria-label="Interview Forge">
        <span sx={styles.brandMark} aria-hidden="true">
          <Icon name="code" size={14} strokeWidth={2.25} />
        </span>
        <span sx={styles.brandName}>Interview Forge</span>
      </Link>
      <span sx={styles.divider} />
      <Control
        appearance={[styles.toolButton, problemOpen && styles.toolPressed]}
        label={problemOpen ? "Hide problem" : "Show problem"}
        tooltip={problemOpen ? "Hide problem" : "Show problem"}
        tooltipAlign="start"
        pressed={problemOpen}
        onActivate={onToggleProblem}
      >
        <Icon name="panel" />
      </Control>
      <span sx={styles.title}>{title}</span>
      {editProblemUrl && (
        <Link
          {...stylex.props(styles.action, styles.runButton, styles.editLink)}
          to={editProblemUrl}
          aria-label="Edit problem"
        >
          Edit
        </Link>
      )}
      <span sx={styles.spacer} />
      <div sx={styles.tools}>
        <Control
          appearance={[styles.toolButton, styles.fontButton, !editorAvailable && styles.toolDisabled]}
          label={`Editor font size: ${fontSize} pixels. Activate to change size.`}
          tooltip="Change editor font size"
          disabled={!editorAvailable}
          onActivate={onCycleFontSize}
        >
          {fontSize} px
        </Control>
        <Control
          appearance={[styles.toolButton, wordWrap && styles.toolPressed, !editorAvailable && styles.toolDisabled]}
          label="Word wrap"
          tooltip={wordWrap ? "Turn off word wrap" : "Turn on word wrap"}
          pressed={wordWrap}
          disabled={!editorAvailable}
          onActivate={onToggleWordWrap}
        >
          <Icon name="wrap" />
        </Control>
        <Control
          appearance={[styles.toolButton, !editorAvailable && styles.toolDisabled]}
          label="Reformat code"
          tooltip="Reformat code indentation. You can undo this change."
          disabled={!editorAvailable}
          onActivate={props.onReformat}
        >
          <Icon name="format" />
        </Control>
        <Control
          appearance={[styles.toolButton, resetDisabled && styles.toolDisabled]}
          label="Reset to starter code"
          tooltip="Reset to starter code. You can undo this change."
          disabled={resetDisabled}
          onActivate={onReset}
        >
          <Icon name="reset" />
        </Control>
        <span sx={[styles.divider, styles.toolDivider]} />
        <Control
          appearance={[styles.action, styles.runButton, runDisabled && styles.actionDisabled]}
          tooltip="Run your code against your test inputs"
          disabled={runDisabled}
          onActivate={onRunTests}
        >
          <Icon name="play" size={12} /> {runLabel}
        </Control>
        <Control
          appearance={[styles.action, styles.submitButton, submitDisabled && styles.actionDisabled]}
          tooltip="Submit against all of the problem’s tests"
          disabled={submitDisabled}
          onActivate={onSubmit}
        >
          <Icon name="submit" size={14} strokeWidth={2.25} /> {submitLabel}
        </Control>
      </div>
    </div>
  );
}

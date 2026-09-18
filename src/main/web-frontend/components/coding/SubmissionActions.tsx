import * as stylex from "@stylexjs/stylex";
import {useId} from "react";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type Props = {
  storageAvailable?: boolean;
  onSubmit?: () => void;
  disabled?: boolean;
  isSubmitting?: boolean;
  isPending?: boolean;
  error?: string | null;
};

const styles = stylex.create({
  controls: {
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#43454a",
    borderRightWidth: {
      default: 1,
      "@media (max-width: 800px)": 0
    },
    borderRightStyle: "solid",
    borderRightColor: "#43454a",
    backgroundColor: "#2b2d30",
    padding: "25px 28px",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    gap: 17
  },
  actionRow: {
    display: "flex",
    gap: 10,
    flexWrap: "wrap"
  },
  action: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    padding: "10px 20px",
    borderRadius: 7,
    fontWeight: 600,
    fontFamily: "inherit",
    fontSize: 13,
    minHeight: 42,
    cursor: "pointer",
    userSelect: "none",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 3
  },
  runButton: {
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    color: "#9da0a8",
    backgroundColor: "#393b40"
  },
  submitButton: {
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#3574f0",
    backgroundColor: "#3574f0",
    color: "#ffffff"
  },
  disabled: {
    opacity: 0.6,
    cursor: "not-allowed"
  },
  error: {
    color: "#f2a6a6",
    fontSize: 12,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  executionNote: {
    color: "#9da0a8",
    fontSize: 12,
    maxWidth: 360,
    lineHeight: 1.7
  },
  saveStatus: {
    display: "flex",
    alignItems: "center",
    gap: 7,
    color: "#89cc8e",
    fontSize: 11
  },
  saveError: {
    color: "#f2c55c"
  }
});

export default function SubmissionActions(props: Props) {
  const {storageAvailable, onSubmit, disabled, isSubmitting, isPending, error} = props;
  const noteId = useId();
  const submitDisabled = disabled || !onSubmit || isSubmitting || isPending;
  const submitLabel = isSubmitting ? "Submitting…" : isPending ? "In progress" : "Submit";

  return (
    <div sx={styles.controls} aria-label="Run and submit your solution">
      <div sx={styles.actionRow}>
        <Control
          disabled
          appearance={[styles.action, styles.runButton, styles.disabled]}
          description={noteId}
        >
          <Icon name="play" size={15} /> Run Tests
        </Control>
        <Control
          disabled={submitDisabled}
          appearance={[styles.action, styles.submitButton, submitDisabled && styles.disabled]}
          onActivate={onSubmit}
        >
          <Icon name="submit" size={15} /> {submitLabel}
        </Control>
      </div>
      <div id={noteId} sx={styles.executionNote}>
        Run Tests is not available yet.
      </div>
      {error && <div role="alert" sx={styles.error}>{error}</div>}
      {storageAvailable !== undefined && (
        <div role="status" sx={[styles.saveStatus, !storageAvailable && styles.saveError]}>
          <Icon name={storageAvailable ? "check" : "document"} size={14} />
          {storageAvailable ? "Drafts are saved on this device" : "Browser storage is unavailable. Copy your code before leaving."}
        </div>
      )}
    </div>
  );
}

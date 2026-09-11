import * as stylex from "@stylexjs/stylex";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type SubmissionActionsProps = {
  storageAvailable: boolean;
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
    cursor: "not-allowed",
    userSelect: "none"
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
    borderColor: "#2e436e",
    backgroundColor: "#2e436e",
    color: "#b5ceff"
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

export default function SubmissionActions({storageAvailable}: SubmissionActionsProps) {
  return (
    <div sx={styles.controls} aria-label="Run and submit your solution">
      <div sx={styles.actionRow}>
        <Control disabled appearance={[styles.action, styles.runButton]} description="execution-note">
          <Icon name="play" size={15} /> Run Tests
        </Control>
        <Control disabled appearance={[styles.action, styles.submitButton]} description="execution-note">
          <Icon name="submit" size={15} /> Submit
        </Control>
      </div>
      <div id="execution-note" sx={styles.executionNote}>
        Run Tests and Submit will be available when code execution is connected.
      </div>
      <div role="status" sx={[styles.saveStatus, !storageAvailable && styles.saveError]}>
        <Icon name={storageAvailable ? "check" : "document"} size={14} />
        {storageAvailable ? "Drafts are saved on this device" : "Browser storage is unavailable. Copy your code before leaving."}
      </div>
    </div>
  );
}

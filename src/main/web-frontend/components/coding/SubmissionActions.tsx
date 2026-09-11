import * as stylex from "@stylexjs/stylex";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type SubmissionActionsProps = {
  storageAvailable: boolean;
};

const styles = stylex.create({
  controls: {
    borderTop: "1px solid #dce1d9",
    borderRight: {
      default: "1px solid #dce1d9",
      "@media (max-width: 800px)": "none"
    },
    backgroundColor: "#fafbf8",
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
    border: "1px solid #cfd7ca",
    color: "#7a8977",
    backgroundColor: "#f2f5ee"
  },
  submitButton: {
    border: "1px solid #56735a",
    backgroundColor: "#56735a",
    color: "#f4f8f0",
    opacity: 0.65
  },
  executionNote: {
    color: "#7e897d",
    fontSize: 12,
    maxWidth: 360,
    lineHeight: 1.7
  },
  saveStatus: {
    display: "flex",
    alignItems: "center",
    gap: 7,
    color: "#69805f",
    fontSize: 11
  },
  saveError: {
    color: "#946a40"
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

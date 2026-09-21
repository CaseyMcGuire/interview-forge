import * as stylex from "@stylexjs/stylex";
import Icon from "./WorkspaceIcon";

type Props = {
  storageAvailable?: boolean;
  cursor: {line: number; column: number} | null;
  languageName?: string;
};

const styles = stylex.create({
  bar: {
    height: 24,
    flexShrink: 0,
    display: "flex",
    alignItems: "center",
    gap: 18,
    padding: "0 14px",
    backgroundColor: "#1e1f22",
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#393b40",
    fontSize: 11,
    lineHeight: 1,
    color: "#9da0a8",
    whiteSpace: "nowrap",
    overflow: "hidden"
  },
  saveStatus: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    minWidth: 0,
    overflow: "hidden",
    textOverflow: "ellipsis",
    color: "#89cc8e"
  },
  saveError: {
    color: "#f2c55c"
  },
  spacer: {
    flexGrow: 1
  },
  language: {
    display: "flex",
    alignItems: "center",
    gap: 6
  },
  languageDot: {
    width: 7,
    height: 7,
    borderRadius: "50%",
    backgroundColor: "#b589ec"
  }
});

/** Editor facts stay out of the editor: draft state, caret position, and language sit on one thin line. */
export default function WorkspaceStatusBar({storageAvailable, cursor, languageName}: Props) {
  return (
    <div sx={styles.bar}>
      {storageAvailable !== undefined && (
        <span role="status" sx={[styles.saveStatus, !storageAvailable && styles.saveError]}>
          <Icon name={storageAvailable ? "check" : "close"} size={12} strokeWidth={2.25} />
          {storageAvailable ? "Drafts saved on this device" : "Browser storage is unavailable. Copy your code before leaving."}
        </span>
      )}
      <span sx={styles.spacer} />
      {cursor && <span>Ln {cursor.line}, Col {cursor.column}</span>}
      <span>Spaces: 2</span>
      <span>UTF-8</span>
      {languageName && (
        <span sx={styles.language}><span sx={styles.languageDot} aria-hidden="true" />{languageName}</span>
      )}
    </div>
  );
}

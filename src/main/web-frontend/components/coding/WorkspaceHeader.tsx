import * as stylex from "@stylexjs/stylex";
import Icon from "./WorkspaceIcon";

const styles = stylex.create({
  header: {
    height: 72,
    flexShrink: 0,
    padding: {
      default: "0 28px",
      "@media (max-width: 600px)": "0 16px"
    },
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 16
  },
  brand: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    fontWeight: 650,
    fontSize: 17,
    letterSpacing: "-0.5px"
  },
  brandMark: {
    display: "grid",
    placeItems: "center",
    width: 30,
    height: 30,
    borderRadius: 8,
    backgroundColor: "#3574f0",
    color: "#ffffff",
    fontFamily: "monospace",
    fontSize: 16
  },
  headerContext: {
    display: "flex",
    alignItems: "center",
    gap: 18,
    fontSize: 12,
    color: "#9da0a8"
  },
  contextLabel: {
    display: {
      default: "inline",
      "@media (max-width: 600px)": "none"
    }
  },
  previewBadge: {
    padding: "4px 10px",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 6,
    color: "#b4b8bf",
    fontSize: 11,
    fontWeight: 600
  }
});

export default function WorkspaceHeader() {
  return (
    <div sx={styles.header}>
      <div sx={styles.brand}>
        <span sx={styles.brandMark} aria-hidden="true">
          <Icon name="code" size={21} />
        </span>
        Interview Forge
      </div>
      <div sx={styles.headerContext}>
        <span sx={styles.contextLabel}>Coding practice</span>
        <span sx={styles.previewBadge}>Editor preview</span>
      </div>
    </div>
  );
}

import * as stylex from "@stylexjs/stylex";
import Icon from "./WorkspaceIcon";

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
    backgroundColor: "#244f3e",
    color: "#e1f2df",
    fontFamily: "monospace",
    fontSize: 16
  },
  headerContext: {
    display: "flex",
    alignItems: "center",
    gap: 18,
    fontSize: 12,
    color: "#68756d"
  },
  contextLabel: {
    display: {
      default: "inline",
      "@media (max-width: 600px)": "none"
    }
  },
  previewBadge: {
    padding: "4px 10px",
    border: "1px solid #d7ddd5",
    borderRadius: 6,
    color: "#627060",
    fontSize: 11,
    fontWeight: 600
  }
});

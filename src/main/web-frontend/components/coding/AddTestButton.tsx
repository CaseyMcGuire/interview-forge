import * as stylex from "@stylexjs/stylex";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type Props = {
  disabled: boolean;
  limitReached: boolean;
  maxCases: number;
  onAdd: () => void;
};

const styles = stylex.create({
  addButton: {
    display: "inline-flex",
    alignItems: "center",
    gap: 5,
    height: 28,
    padding: "0 10px 0 7px",
    borderRadius: 6,
    color: {
      default: "#9da0a8",
      ":hover": "#dfe1e5"
    },
    fontSize: 12,
    fontWeight: 500,
    whiteSpace: "nowrap",
    cursor: "pointer",
    userSelect: "none",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 1
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

export default function AddTestButton(props: Props) {
  return (
    <Control
      appearance={[styles.addButton, props.disabled && styles.disabled]}
      disabled={props.disabled}
      title={props.limitReached ? `Up to ${props.maxCases} tests` : undefined}
      onActivate={props.onAdd}
    >
      <Icon name="plus" size={13} strokeWidth={2} /> Add test
    </Control>
  );
}

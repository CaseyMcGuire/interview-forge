import * as stylex from "@stylexjs/stylex";
import {useSyncExternalStore, type ReactNode} from "react";
import {Group, Panel, Separator} from "react-resizable-panels";

type Props = {
  problem: ReactNode;
  editor: ReactNode;
  actions: ReactNode;
  results: ReactNode;
};

const narrowViewportQuery = "(max-width: 800px)";

const styles = stylex.create({
  workspace: {
    display: "flex",
    flex: 1,
    minHeight: 0,
    margin: {
      default: "0 20px 20px",
      "@media (max-width: 600px)": "0 8px 8px"
    },
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#43454a",
    borderRadius: 12,
    overflow: "hidden",
    backgroundColor: "#2b2d30",
    boxShadow: "0 4px 20px #00000026"
  },
  stacked: {
    display: "grid",
    gridTemplateColumns: "minmax(0, 1fr)",
    gridTemplateRows: "auto 450px auto auto"
  },
  group: {
    minWidth: 0,
    minHeight: 0
  },
  column: {
    display: "grid",
    gridTemplateRows: "minmax(0, 1fr) 254px",
    minWidth: 0,
    minHeight: 0
  },
  separator: {
    width: 8,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#25262a",
    color: "#9da0a8",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #9da0a8"
    },
    outlineOffset: -2
  },
  grip: {
    height: 32,
    width: 2,
    borderRadius: 1,
    backgroundColor: "currentColor",
    pointerEvents: "none"
  }
});

export default function WorkspaceLayout(props: Props) {
  const isNarrow = useSyncExternalStore(subscribeToViewportChanges, isNarrowViewport);

  // Keep the existing reading order on mobile: problem, editor, actions, then results.
  if (isNarrow) {
    return (
      <div sx={[styles.workspace, styles.stacked]} role="main">
        {props.problem}
        {props.editor}
        {props.actions}
        {props.results}
      </div>
    );
  }

  return (
    <div sx={styles.workspace} role="main">
      <Group orientation="horizontal" {...stylex.props(styles.group)}>
        <Panel defaultSize="43%" minSize="280px" {...stylex.props(styles.column)}>
          {props.problem}
          {props.actions}
        </Panel>

        <Separator
          aria-label="Resize problem and code panels"
          title="Drag to resize panels; double-click to reset"
          {...stylex.props(styles.separator)}
        >
          <div sx={styles.grip} aria-hidden="true" />
        </Separator>

        <Panel defaultSize="57%" minSize="320px" {...stylex.props(styles.column)}>
          {props.editor}
          {props.results}
        </Panel>
      </Group>
    </div>
  );
}

function isNarrowViewport() {
  return window.matchMedia(narrowViewportQuery).matches;
}

function subscribeToViewportChanges(onChange: () => void) {
  const mediaQuery = window.matchMedia(narrowViewportQuery);
  mediaQuery.addEventListener("change", onChange);

  return () => mediaQuery.removeEventListener("change", onChange);
}

import * as stylex from "@stylexjs/stylex";
import {useSyncExternalStore, type ReactNode} from "react";
import {Group, Panel, Separator} from "react-resizable-panels";

type Props = {
  /** The problem sidebar, or null when it is hidden. */
  problem: ReactNode | null;
  editor: ReactNode;
};

const narrowViewportQuery = "(max-width: 800px)";

const styles = stylex.create({
  body: {
    display: "flex",
    flexGrow: 1,
    minWidth: 0,
    minHeight: 0
  },
  stacked: {
    flexDirection: "column"
  },
  group: {
    flexGrow: 1,
    minWidth: 0,
    minHeight: 0
  },
  panel: {
    display: "flex",
    flexDirection: "column",
    minWidth: 0,
    minHeight: 0
  },
  separator: {
    width: 8,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#1e1f22",
    borderRightWidth: 1,
    borderRightStyle: "solid",
    borderRightColor: "#393b40",
    color: {
      default: "#6b6e76",
      ":hover": "#9da0a8"
    },
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
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

/** The problem sidebar and the editor column share a draggable divider; on narrow screens they stack. */
export default function WorkspaceLayout({problem, editor}: Props) {
  const isNarrow = useSyncExternalStore(subscribeToViewportChanges, isNarrowViewport);

  if (problem === null) {
    return <div sx={styles.body}>{editor}</div>;
  }

  // Keep the reading order on mobile: problem first, then the editor and its tests.
  if (isNarrow) {
    return (
      <div sx={[styles.body, styles.stacked]}>
        {problem}
        {editor}
      </div>
    );
  }

  return (
    <div sx={styles.body}>
      <Group orientation="horizontal" {...stylex.props(styles.group)}>
        <Panel defaultSize="26%" minSize="280px" maxSize="50%" {...stylex.props(styles.panel)}>
          {problem}
        </Panel>

        <Separator
          aria-label="Resize problem and code panels"
          title="Drag to resize panels; double-click to reset"
          {...stylex.props(styles.separator)}
        >
          <div sx={styles.grip} aria-hidden="true" />
        </Separator>

        <Panel defaultSize="74%" minSize="320px" {...stylex.props(styles.panel)}>
          {editor}
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

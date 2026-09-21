import * as stylex from "@stylexjs/stylex";
import type {TestCaseTone} from "./testCaseRows";

type Props = {
  tone: TestCaseTone;
};

const styles = stylex.create({
  dot: {
    width: 8,
    height: 8,
    flexShrink: 0,
    boxSizing: "border-box",
    borderRadius: "50%",
    borderWidth: 1.5,
    borderStyle: "solid",
    borderColor: "transparent"
  },
  dotIdle: {
    borderColor: "#6b6e76"
  },
  dotRunning: {
    backgroundColor: "#6ea1ff",
    borderColor: "#6ea1ff"
  },
  dotPassed: {
    backgroundColor: "#89cc8e",
    borderColor: "#89cc8e"
  },
  dotFailed: {
    backgroundColor: "#f2a6a6",
    borderColor: "#f2a6a6"
  }
});

const toneStyles = {
  idle: styles.dotIdle,
  running: styles.dotRunning,
  passed: styles.dotPassed,
  failed: styles.dotFailed
};

export default function TestStatusDot(props: Props) {
  return <span sx={[styles.dot, toneStyles[props.tone]]} aria-hidden="true" />;
}

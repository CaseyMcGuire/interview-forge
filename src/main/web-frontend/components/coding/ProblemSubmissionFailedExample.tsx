import * as stylex from "@stylexjs/stylex";
import {graphql, useFragment} from "react-relay";
import type {ProblemSubmissionFailedExample_example$key} from "__generated__/ProblemSubmissionFailedExample_example.graphql";
import {formatTestCaseJson} from "./formatTestCaseJson";

type Props = {
  example: ProblemSubmissionFailedExample_example$key;
};

const styles = stylex.create({
  heading: {
    margin: "16px 0 8px",
    fontSize: 12,
    fontWeight: 600
  },
  values: {
    display: "grid",
    gridTemplateColumns: {
      default: "repeat(3, minmax(0, 1fr))",
      "@media (max-width: 1100px)": "minmax(0, 1fr)"
    },
    gap: 10
  },
  block: {
    backgroundColor: "#1e1f22",
    border: "1px solid #393b40",
    borderRadius: 5,
    padding: "9px 12px",
    minWidth: 0
  },
  label: {
    fontSize: 11,
    fontWeight: 600,
    color: "#9da0a8",
    marginBottom: 4
  },
  value: {
    margin: 0,
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    color: "#bcbec4",
    lineHeight: 1.7,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  empty: {
    color: "#9da0a8",
    fontSize: 12
  }
});

export default function ProblemSubmissionFailedExample(props: Props) {
  const example = useFragment(graphql`
    fragment ProblemSubmissionFailedExample_example on ProblemSubmissionFailedExample {
      inputJson
      expectedOutputJson
      output
    }
  `, props.example);

  return (
    <section aria-label="Failed example">
      <h3 sx={styles.heading}>Failed example</h3>

      <div sx={styles.values}>
        <div sx={styles.block}>
          <div sx={styles.label}>Input</div>
          <pre sx={styles.value}>{formatTestCaseJson(example.inputJson)}</pre>
        </div>

        <div sx={styles.block}>
          <div sx={styles.label}>Expected output</div>
          <pre sx={styles.value}>{formatTestCaseJson(example.expectedOutputJson)}</pre>
        </div>

        <div sx={styles.block}>
          <div sx={styles.label}>Your output</div>
          {example.output.length > 0 ? (
            <pre sx={styles.value}>{example.output}</pre>
          ) : (
            <div sx={styles.empty}>No output</div>
          )}
        </div>
      </div>
    </section>
  );
}

import * as stylex from "@stylexjs/stylex";
import {graphql, useFragment} from "react-relay";
import type {ProblemSubmissionFailedExample_example$key} from "__generated__/ProblemSubmissionFailedExample_example.graphql";
import {formatTestCaseJson} from "./formatTestCaseJson";

type Props = {
  example: ProblemSubmissionFailedExample_example$key;
};

const styles = stylex.create({
  section: {
    display: "flex",
    flexDirection: "column",
    gap: 10
  },
  heading: {
    margin: "4px 0 0",
    fontSize: 11,
    fontWeight: 600,
    letterSpacing: "0.3px",
    textTransform: "uppercase",
    color: "#9da0a8"
  },
  values: {
    display: "grid",
    gridTemplateColumns: "96px minmax(0, 1fr)",
    rowGap: 6,
    columnGap: 12,
    margin: 0
  },
  label: {
    fontSize: 12,
    color: "#9da0a8"
  },
  value: {
    margin: 0,
    minWidth: 0,
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 13,
    lineHeight: 1.5,
    color: "#dfe1e5",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  wrong: {
    color: "#f2a6a6"
  },
  empty: {
    fontFamily: "inherit",
    color: "#9da0a8"
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
    <section aria-label="Failed case" sx={styles.section}>
      <h3 sx={styles.heading}>Failed case</h3>
      <dl sx={styles.values}>
        <dt sx={styles.label}>Input</dt>
        <dd sx={styles.value}>{formatTestCaseJson(example.inputJson)}</dd>
        <dt sx={styles.label}>Expected</dt>
        <dd sx={styles.value}>{formatTestCaseJson(example.expectedOutputJson)}</dd>
        <dt sx={styles.label}>Your output</dt>
        <dd sx={[styles.value, styles.wrong, example.output.length === 0 && styles.empty]}>
          {example.output.length > 0 ? example.output : "No output"}
        </dd>
      </dl>
    </section>
  );
}

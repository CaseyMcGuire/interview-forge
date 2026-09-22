import * as stylex from "@stylexjs/stylex";
import ProblemDifficultyBadge from "components/problems/ProblemDifficultyBadge";
import type {CodingProblem} from "./codingProblemTypes";
import {formatTestCaseJson} from "./formatTestCaseJson";
import ProblemMarkdown from "./ProblemMarkdown";

type ProblemPanelProps = {
  problem: CodingProblem;
};

const styles = stylex.create({
  problem: {
    flexGrow: {
      default: 1,
      "@media (max-width: 800px)": 0
    },
    maxHeight: {
      default: "none",
      "@media (max-width: 800px)": "45vh"
    },
    flexShrink: 0,
    minHeight: 0,
    minWidth: 0,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#1e1f22",
    borderBottomWidth: {
      default: 0,
      "@media (max-width: 800px)": 1
    },
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40"
  },
  problemBody: {
    overflowY: "auto",
    padding: "22px 26px 28px 28px",
    display: "flex",
    flexDirection: "column",
    gap: 14,
    scrollbarWidth: "thin",
    scrollbarColor: "#4e5157 transparent",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: -2
  },
  title: {
    margin: 0,
    fontSize: 20,
    lineHeight: 1.3,
    letterSpacing: "-0.3px",
    fontWeight: 650,
    color: "#dfe1e5"
  },
  badges: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    flexWrap: "wrap"
  },
  sectionHeading: {
    margin: "8px 0 0",
    fontSize: 12,
    fontWeight: 650,
    color: "#dfe1e5"
  },
  example: {
    display: "flex",
    flexDirection: "column",
    gap: 6
  },
  exampleTitle: {
    fontSize: 11,
    fontWeight: 600,
    color: "#9da0a8"
  },
  exampleValues: {
    margin: 0,
    padding: "9px 12px",
    backgroundColor: "#242528",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#393b40",
    borderRadius: 6
  },
  exampleRow: {
    display: "grid",
    gridTemplateColumns: "52px minmax(0, 1fr)",
    columnGap: 10
  },
  exampleOutput: {
    marginTop: 8,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#393b40"
  },
  exampleLabel: {
    alignSelf: "start",
    lineHeight: 1.6,
    fontSize: 11,
    fontWeight: 600,
    color: "#9da0a8"
  },
  exampleValue: {
    margin: 0,
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    lineHeight: 1.6,
    color: "#dfe1e5",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  exampleExplanation: {
    fontSize: 12.5,
    lineHeight: 1.55
  }
});

/** The problem reads as a quiet sidebar: no panel chrome, examples as label/value blocks. */
export default function ProblemPanel({problem}: ProblemPanelProps) {
  return (
    <div sx={styles.problem} role="region" aria-labelledby="problem-title">
      <div sx={styles.problemBody} tabIndex={0} aria-label="Problem description">
        <div role="heading" aria-level={1} id="problem-title" sx={styles.title}>{problem.title}</div>
        <div sx={styles.badges}>
          <ProblemDifficultyBadge difficulty={problem.difficulty} />
        </div>
        <ProblemMarkdown>{problem.statementMarkdown}</ProblemMarkdown>

        {problem.examples.length > 0 && (
          <div role="heading" aria-level={2} sx={styles.sectionHeading}>Examples</div>
        )}
        {problem.examples.map((item, index) => (
          <div sx={styles.example} key={item.id}>
            <div sx={styles.exampleTitle}>Example {index + 1}</div>
            <dl sx={styles.exampleValues}>
              <div sx={styles.exampleRow}>
                <dt sx={styles.exampleLabel}>Input</dt>
                <dd sx={styles.exampleValue}>{formatTestCaseJson(item.inputJson)}</dd>
              </div>
              <div sx={[styles.exampleRow, styles.exampleOutput]}>
                <dt sx={styles.exampleLabel}>Output</dt>
                <dd sx={styles.exampleValue}>{formatTestCaseJson(item.expectedOutputJson)}</dd>
              </div>
            </dl>
            {item.explanationMarkdown && (
              <div sx={styles.exampleExplanation}><ProblemMarkdown>{item.explanationMarkdown}</ProblemMarkdown></div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

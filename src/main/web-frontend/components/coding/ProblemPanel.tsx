import * as stylex from "@stylexjs/stylex";
import ProblemDifficultyBadge from "components/problems/ProblemDifficultyBadge";
import type {CodingProblem} from "./codingProblemTypes";
import {formatTestCaseJson} from "./formatTestCaseJson";
import ProblemMarkdown from "./ProblemMarkdown";
import Icon from "./WorkspaceIcon";

type ProblemPanelProps = {
  problem: CodingProblem;
};

const styles = stylex.create({
  problem: {
    minHeight: 0,
    minWidth: 0,
    display: "flex",
    flexDirection: "column"
  },
  panelHeading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    minHeight: 49,
    padding: "0 24px",
    borderBottomWidth: 1,
    borderBottomStyle: "solid",
    borderBottomColor: "#393b40",
    flexShrink: 0
  },
  panelLabel: {
    display: "flex",
    alignItems: "center",
    gap: 9,
    fontSize: 12,
    fontWeight: 600
  },
  problemBody: {
    overflowY: "auto",
    padding: {
      default: "28px 32px 32px",
      "@media (max-width: 1100px)": "24px"
    },
    scrollbarWidth: "thin",
    scrollbarColor: "#4e5157 transparent",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: -2
  },
  title: {
    fontSize: 29,
    lineHeight: 1.25,
    letterSpacing: "-0.9px",
    fontWeight: 650,
    marginTop: 8,
    marginBottom: 14
  },
  badges: {
    display: "flex",
    alignItems: "center",
    gap: 7,
    flexWrap: "wrap",
    marginBottom: 26
  },
  sectionHeading: {
    fontSize: 13,
    fontWeight: 650,
    marginTop: 25,
    marginBottom: 12,
    color: "#dfe1e5"
  },
  example: {
    borderLeftWidth: 2,
    borderLeftStyle: "solid",
    borderLeftColor: "#4e5157",
    padding: "1px 0 1px 16px",
    marginBottom: 23
  },
  exampleTitle: {
    fontSize: 11,
    color: "#9da0a8",
    fontWeight: 600,
    marginBottom: 7
  },
  exampleCode: {
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    lineHeight: 1.9,
    color: "#bcbec4",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  exampleExplanation: {
    fontSize: 12,
    color: "#9da0a8",
    lineHeight: 1.7,
    marginTop: 7
  }
});

export default function ProblemPanel({problem}: ProblemPanelProps) {
  return (
    <div sx={styles.problem} role="region" aria-labelledby="problem-title">
      <div sx={styles.panelHeading}>
        <span sx={styles.panelLabel}><Icon name="document" /> Problem</span>
      </div>
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
            <div sx={styles.exampleCode}>{`Input: ${formatTestCaseJson(item.inputJson)}\nExpected output: ${formatTestCaseJson(item.expectedOutputJson)}`}</div>
            {item.explanationMarkdown && (
              <div sx={styles.exampleExplanation}><ProblemMarkdown>{item.explanationMarkdown}</ProblemMarkdown></div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

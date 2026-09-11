import * as stylex from "@stylexjs/stylex";
import {useState} from "react";
import type {ExampleTestCase} from "./codingProblemTypes";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type TestResultsPanelProps = {
  examples: readonly ExampleTestCase[];
};

const styles = stylex.create({
  panelHeading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    minHeight: 49,
    padding: "0 24px",
    borderBottom: "1px solid #e8ece5",
    flexShrink: 0
  },
  panelLabel: {
    display: "flex",
    alignItems: "center",
    gap: 9,
    fontSize: 12,
    fontWeight: 600
  },
  results: {
    borderTop: "1px solid #dce1d9",
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column"
  },
  resultsHeading: {
    minHeight: 45,
    padding: "0 22px"
  },
  notRun: {
    fontSize: 10,
    padding: "2px 7px",
    borderRadius: 4,
    backgroundColor: "#f3f4f0",
    color: "#7d8678"
  },
  resultsBody: {
    padding: "16px 22px",
    overflowY: "auto",
    scrollbarWidth: "thin"
  },
  caseTabs: {
    display: "flex",
    gap: 6,
    marginBottom: 15
  },
  caseButton: {
    padding: "5px 11px",
    border: "1px solid transparent",
    borderRadius: 5,
    backgroundColor: "transparent",
    color: "#7d897c",
    fontSize: 11,
    cursor: "pointer",
    fontFamily: "inherit",
    userSelect: "none"
  },
  selectedCase: {
    backgroundColor: "#edf3e8",
    color: "#4a6944",
    borderColor: "#dce6d4",
    fontWeight: 600
  },
  caseData: {
    display: "grid",
    gridTemplateColumns: {
      default: "minmax(0, 1fr) minmax(0, 1fr)",
      "@media (max-width: 420px)": "minmax(0, 1fr)"
    },
    gap: 12
  },
  dataBlock: {
    backgroundColor: "#f7f8f4",
    border: "1px solid #ecefe7",
    padding: "9px 12px",
    borderRadius: 5,
    minWidth: 0
  },
  dataLabel: {
    fontSize: 10,
    fontWeight: 600,
    color: "#87917f",
    marginBottom: 4
  },
  dataValue: {
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 11,
    color: "#586b50",
    lineHeight: 1.9,
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  resultsHint: {
    marginTop: 11,
    fontSize: 11,
    color: "#8c9386",
    display: "flex",
    alignItems: "center",
    gap: 7
  }
});

export default function TestResultsPanel({examples}: TestResultsPanelProps) {
  const [selectedCase, setSelectedCase] = useState(0);
  const example = examples[selectedCase];

  return (
    <div sx={styles.results} role="region" aria-labelledby="results-title">
      <div sx={[styles.panelHeading, styles.resultsHeading]}>
        <div role="heading" aria-level={2} id="results-title" sx={styles.panelLabel}>
          <Icon name="terminal" /> Test Results
        </div>
        <span sx={styles.notRun}>Not run</span>
      </div>
      <div sx={styles.resultsBody}>
        <div sx={styles.caseTabs} role="group" aria-label="Example test cases">
          {examples.map((_, index) => (
            <Control
              key={index}
              appearance={[styles.caseButton, index === selectedCase && styles.selectedCase]}
              pressed={index === selectedCase}
              controls="selected-case"
              onActivate={() => setSelectedCase(index)}
            >
              Case {index + 1}
            </Control>
          ))}
        </div>
        <div id="selected-case" sx={styles.caseData} aria-live="polite" aria-label={`Case ${selectedCase + 1}`}>
          <div sx={styles.dataBlock}>
            <div sx={styles.dataLabel}>Input</div>
            <div sx={styles.dataValue}>{`nums = ${example.nums}\ntarget = ${example.target}`}</div>
          </div>
          <div sx={styles.dataBlock}>
            <div sx={styles.dataLabel}>Expected output</div>
            <div sx={styles.dataValue}>{example.expected}</div>
          </div>
        </div>
        <div sx={styles.resultsHint}>
          <Icon name="clock" size={13} /> Your test results will appear here after a run.
        </div>
      </div>
    </div>
  );
}

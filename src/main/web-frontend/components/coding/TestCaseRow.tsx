import * as stylex from "@stylexjs/stylex";
import {useId} from "react";
import TestStatusDot from "./TestStatusDot";
import type {TestCaseRowData} from "./testCaseRows";
import Control from "./WorkspaceControl";
import Icon from "./WorkspaceIcon";

type Props = {
  row: TestCaseRowData;
  open: boolean;
  disabled: boolean;
  removeDisabled: boolean;
  fieldError?: string;
  descriptionId: string;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onRemove: () => void;
};

type OutcomeProps = {
  row: TestCaseRowData;
};

const monospace = '"SFMono-Regular", Consolas, monospace';

const styles = stylex.create({
  row: {
    display: "flex",
    flexDirection: "column",
    borderTopWidth: {
      default: 1,
      ":first-child": 0
    },
    borderTopStyle: "solid",
    borderTopColor: "#2a2b2f"
  },
  rowOpen: {
    backgroundColor: "#222327"
  },
  rowMain: {
    display: "flex",
    alignItems: "center",
    gap: 12,
    minHeight: 40,
    padding: "0 8px 0 16px"
  },
  summary: {
    display: "flex",
    alignItems: "center",
    gap: 12,
    flexGrow: 1,
    minWidth: 0,
    minHeight: 40
  },
  iconButton: {
    display: "grid",
    placeItems: "center",
    width: 28,
    height: 28,
    flexShrink: 0,
    borderRadius: 6,
    color: {
      default: "#9da0a8",
      ":hover": "#dfe1e5"
    },
    cursor: "pointer",
    userSelect: "none",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 1
  },
  actions: {
    display: "flex",
    alignItems: "center",
    gap: 2
  },
  removeButton: {
    color: {
      default: "#6b6e76",
      ":hover": "#dfe1e5"
    }
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  caseLabel: {
    width: 52,
    flexShrink: 0,
    fontSize: 12,
    color: "#9da0a8"
  },
  inputText: {
    flexGrow: 1,
    minWidth: 0,
    fontFamily: monospace,
    fontSize: 13,
    color: "#dfe1e5",
    textAlign: "left",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis"
  },
  inputEmpty: {
    fontFamily: "inherit",
    fontStyle: "italic",
    color: "#9da0a8"
  },
  result: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    width: {
      default: 200,
      "@media (max-width: 1000px)": 120
    },
    flexShrink: 0,
    minWidth: 0,
    fontSize: 12,
    color: "#9da0a8"
  },
  resultText: {
    minWidth: 0,
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis"
  },
  resultPassed: {
    fontFamily: monospace,
    fontSize: 13,
    color: "#89cc8e"
  },
  resultFailed: {
    fontWeight: 600,
    color: "#f2a6a6"
  },
  details: {
    display: "grid",
    gridTemplateColumns: "96px minmax(0, 1fr)",
    rowGap: 6,
    columnGap: 12,
    margin: 0,
    padding: {
      default: "2px 16px 14px 100px",
      "@media (max-width: 800px)": "2px 16px 14px 16px"
    }
  },
  detailLabel: {
    fontSize: 12,
    color: "#9da0a8"
  },
  detailValue: {
    margin: 0,
    minWidth: 0,
    fontFamily: monospace,
    fontSize: 13,
    lineHeight: 1.5,
    color: "#dfe1e5",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  detailPassed: {
    color: "#89cc8e"
  },
  detailFailed: {
    color: "#f2a6a6"
  },
  detailMuted: {
    fontFamily: "inherit",
    color: "#9da0a8"
  },
  detailError: {
    padding: {
      default: "0 16px 12px 100px",
      "@media (max-width: 800px)": "0 16px 12px 16px"
    },
    fontSize: 12,
    lineHeight: 1.5,
    color: "#f2a6a6",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  }
});

function TestCaseOutcome(props: OutcomeProps) {
  const {row} = props;
  if (!row.resultText) {
    return null;
  }

  return (
    <span sx={[styles.result, row.tone === "passed" && styles.resultPassed, row.tone === "failed" && styles.resultFailed]}>
      {row.tone === "passed" && <Icon name="check" size={13} strokeWidth={2.25} />}
      {row.tone === "failed" && <Icon name="failed" size={14} />}
      <span sx={styles.resultText}>{row.resultText}</span>
    </span>
  );
}

export default function TestCaseRow(props: Props) {
  const {row, open, disabled, removeDisabled, fieldError} = props;
  const errorId = useId();
  const number = row.index + 1;
  const {result} = row;
  // Serialized values render verbatim: parsing them would hide 1 versus 1.0 and round large integers.
  const showValues = open && result !== null && result.outcome !== "NOT_RUN";
  const hasDetails = result !== null && (result.outcome !== "NOT_RUN" || Boolean(result.publicErrorMessage));
  const showDetails = showValues || (open && Boolean(result?.publicErrorMessage));

  return (
    <div sx={[styles.row, (showDetails || Boolean(fieldError)) && styles.rowOpen]}>
      <div sx={styles.rowMain}>
        <div sx={styles.summary}>
          <TestStatusDot tone={row.tone} />
          <span sx={styles.caseLabel}>Test {number}</span>
          <span sx={[styles.inputText, row.draft.inputJson.length === 0 && styles.inputEmpty]}>
            {row.draft.inputJson.length > 0 ? row.draft.inputJson : "Empty input"}
          </span>
          <TestCaseOutcome row={row} />
        </div>
        {hasDetails && (
          <Control
            appearance={styles.iconButton}
            label={`${open ? "Collapse" : "Show"} test ${number} results`}
            expanded={open}
            onActivate={() => props.onOpenChange(!open)}
          >
            <Icon name={open ? "chevronUp" : "chevronDown"} />
          </Control>
        )}
        <div sx={styles.actions}>
          <Control
            appearance={[styles.iconButton, disabled && styles.disabled]}
            label={`Edit test ${number} input`}
            description={fieldError ? errorId : props.descriptionId}
            title="Edit formatted input"
            disabled={disabled}
            onActivate={props.onEdit}
          >
            <Icon name="edit" size={14} />
          </Control>
          <Control
            appearance={[styles.iconButton, styles.removeButton, removeDisabled && styles.disabled]}
            label={`Remove test ${number}`}
            disabled={removeDisabled}
            onActivate={props.onRemove}
          >
            <Icon name="close" size={14} />
          </Control>
        </div>
      </div>
      {fieldError && <div id={errorId} role="alert" sx={styles.detailError}>{fieldError}</div>}
      {open && result?.publicErrorMessage && <div sx={styles.detailError}>{result.publicErrorMessage}</div>}
      {showValues && (
        <dl sx={styles.details}>
          <dt sx={styles.detailLabel}>Expected</dt>
          <dd sx={[styles.detailValue, result.testCase.expectedOutputJson === null && styles.detailMuted]}>
            {result.testCase.expectedOutputJson ?? "Not available"}
          </dd>
          <dt sx={styles.detailLabel}>Your output</dt>
          <dd sx={[
            styles.detailValue,
            row.tone === "passed" && styles.detailPassed,
            row.tone === "failed" && styles.detailFailed,
            result.output.length === 0 && styles.detailMuted
          ]}>
            {result.output.length > 0 ? result.output : "No output"}
          </dd>
        </dl>
      )}
    </div>
  );
}

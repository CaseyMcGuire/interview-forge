import * as stylex from "@stylexjs/stylex";
import {useId, useState, type ReactNode} from "react";
import {graphql, useFragment} from "react-relay";
import type {TestPanel_submission$key} from "__generated__/TestPanel_submission.graphql";
import {AppRoutes} from "routes/AppRoutes";
import AddTestButton from "./AddTestButton";
import TestCaseRow from "./TestCaseRow";
import TestInputDialog from "./TestInputDialog";
import TestPanelHeader, {type SubmissionChip, type WorkspacePanel} from "./TestPanelHeader";
import {createTestCaseRow, type CustomInputDraft, type TestCaseRowData} from "./testCaseRows";
import Control from "./WorkspaceControl";

export type WorkspaceNotice = {
  id: string;
  message: string;
  requiresSignIn?: boolean;
};

type Props = {
  panel: WorkspacePanel;
  onSelectPanel: (panel: WorkspacePanel) => void;
  expanded: boolean;
  onToggleExpanded: () => void;
  cases: readonly CustomInputDraft[];
  maxCases: number;
  onCasesChange: (cases: CustomInputDraft[]) => void;
  disabled?: boolean;
  errors?: Readonly<Record<string, string>>;
  submission: TestPanel_submission$key | null;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  submissionChip: SubmissionChip;
  notices: readonly WorkspaceNotice[];
  children: ReactNode;
};

const styles = stylex.create({
  panel: {
    flexShrink: 0,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#1e1f22",
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#393b40"
  },
  body: {
    height: {
      default: 244,
      "@media (max-width: 800px)": 300
    },
    display: "flex",
    flexDirection: "column",
    overflowY: "auto",
    scrollbarWidth: "thin",
    scrollbarColor: "#4e5157 transparent"
  },
  notice: {
    padding: "10px 16px 0",
    fontSize: 12,
    color: "#f2a6a6",
    overflowWrap: "anywhere"
  },
  signIn: {
    display: "inline-block",
    marginTop: 4,
    color: "#b5ceff"
  },
  retry: {
    width: "fit-content",
    marginTop: 8,
    padding: "5px 10px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    color: "#dfe1e5",
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  runError: {
    margin: "10px 16px 0",
    padding: "8px 12px",
    borderRadius: 5,
    backgroundColor: "#16171a",
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    lineHeight: 1.5,
    color: "#f2a6a6",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere"
  },
  rows: {
    display: "flex",
    flexDirection: "column"
  },
  addRow: {
    display: "flex",
    alignItems: "center",
    minHeight: 40,
    padding: "0 8px 0 12px",
    borderTopWidth: 1,
    borderTopStyle: "solid",
    borderTopColor: "#2a2b2f"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  screenReader: {
    position: "absolute",
    width: 1,
    height: 1,
    padding: 0,
    margin: -1,
    overflow: "hidden",
    clipPath: "inset(50%)",
    whiteSpace: "nowrap",
    borderWidth: 0
  }
});

/** Coordinates test inputs, results, and expansion state across the tests and submission views. */
export default function TestPanel(props: Props) {
  const {panel, onSelectPanel, expanded, onToggleExpanded, submissionChip, notices, children} = props;
  const {cases, maxCases, onCasesChange, disabled = false, errors = {}, isLoading = false, error, onRetry} = props;
  const submission = useFragment(graphql`
    fragment TestPanel_submission on CustomInputSubmission {
      status
      outcome
      totalCases
      passedCases
      runtimeMs
      publicErrorMessage
      caseResults {
        outcome
        output
        publicErrorMessage
        testCase {
          id
          position
          inputJson
          expectedOutputJson
        }
      }
    }
  `, props.submission);

  const id = useId();
  const bodyId = `${id}-body`;
  const descriptionId = `${id}-description`;
  const [openOverrides, setOpenOverrides] = useState<Record<string, boolean>>({});
  const [editingCaseId, setEditingCaseId] = useState<string | null>(null);
  const editingCaseIndex = cases.findIndex(testCase => testCase.id === editingCaseId);
  const editingCase = cases[editingCaseIndex];
  const isPending = submission?.status === "QUEUED" || submission?.status === "RUNNING";
  const results = submission?.status === "FINISHED" ? submission.caseResults ?? null : null;
  const rows = cases.map((draft, index) => createTestCaseRow(draft, index, results, isPending));
  const addDisabled = disabled || cases.length >= maxCases;
  const removeDisabled = disabled || cases.length <= 1;

  function isOpen(row: TestCaseRowData) {
    return openOverrides[row.draft.id] ?? (row.tone === "failed" || Boolean(errors[row.draft.id]));
  }

  function setOpen(caseId: string, open: boolean) {
    setOpenOverrides(previous => ({...previous, [caseId]: open}));
  }

  function addCase() {
    if (addDisabled) {
      return;
    }

    const caseId = crypto.randomUUID();
    onCasesChange([...cases, {id: caseId, inputJson: ""}]);
    setEditingCaseId(caseId);
  }

  function updateCase(caseId: string, inputJson: string) {
    onCasesChange(cases.map(testCase => testCase.id === caseId ? {...testCase, inputJson} : testCase));
  }

  function removeCase(caseId: string) {
    if (removeDisabled) {
      return;
    }

    onCasesChange(cases.filter(testCase => testCase.id !== caseId));
  }

  return (
    <section sx={styles.panel} aria-label="Tests and submission">
      <TestPanelHeader
        panel={panel}
        onSelectPanel={onSelectPanel}
        expanded={expanded}
        onToggleExpanded={onToggleExpanded}
        bodyId={bodyId}
        rows={rows}
        maxCases={maxCases}
        addDisabled={addDisabled}
        onAddCase={addCase}
        submission={submission}
        submissionChip={submissionChip}
        isLoading={isLoading}
        hasError={Boolean(error)}
      />

      {expanded && (
        <div id={bodyId} sx={styles.body}>
          {notices.map(notice => (
            <div key={notice.id} sx={styles.notice}>
              <div role="alert">{notice.message}</div>
              {notice.requiresSignIn && <a href={AppRoutes.Login()} sx={styles.signIn}>Sign in to continue</a>}
            </div>
          ))}

          {panel === "submission" ? children : (
            <>
              {error && (
                <div sx={styles.notice}>
                  <div role="alert">{error}</div>
                  {onRetry && (
                    <Control
                      appearance={[styles.retry, isLoading && styles.disabled]}
                      disabled={isLoading}
                      onActivate={onRetry}
                    >
                      {isLoading ? "Checking results…" : "Retry loading results"}
                    </Control>
                  )}
                </div>
              )}
              {submission?.publicErrorMessage && <pre sx={styles.runError}>{submission.publicErrorMessage}</pre>}
              <p id={descriptionId} sx={styles.screenReader}>
                Enter one JSON value per test. Expected outputs are calculated automatically.
              </p>
              <div sx={styles.rows}>
                {rows.map(row => (
                  <TestCaseRow
                    key={row.draft.id}
                    row={row}
                    open={isOpen(row)}
                    disabled={disabled}
                    removeDisabled={removeDisabled}
                    fieldError={errors[row.draft.id]}
                    descriptionId={descriptionId}
                    onOpenChange={open => setOpen(row.draft.id, open)}
                    onEdit={() => setEditingCaseId(row.draft.id)}
                    onRemove={() => removeCase(row.draft.id)}
                  />
                ))}
              </div>
              <div sx={styles.addRow}>
                <AddTestButton
                  disabled={addDisabled}
                  limitReached={cases.length >= maxCases}
                  maxCases={maxCases}
                  onAdd={addCase}
                />
              </div>
            </>
          )}
        </div>
      )}
      {editingCase && (
        <TestInputDialog
          key={editingCase.id}
          testNumber={editingCaseIndex + 1}
          inputJson={editingCase.inputJson}
          disabled={disabled}
          onSave={inputJson => updateCase(editingCase.id, inputJson)}
          onClose={() => setEditingCaseId(null)}
        />
      )}
    </section>
  );
}

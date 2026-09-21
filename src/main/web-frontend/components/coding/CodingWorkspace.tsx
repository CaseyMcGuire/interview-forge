import * as stylex from "@stylexjs/stylex";
import {useState} from "react";
import {useSearchParams} from "react-router";
import {AppRoutes} from "routes/AppRoutes";
import useSubmitSolution from "hooks/useSubmitSolution";
import useProblemSubmissionStatus from "hooks/useProblemSubmissionStatus";
import useEnqueueCustomInputSubmission from "hooks/useEnqueueCustomInputSubmission";
import useCustomInputSubmissionStatus from "hooks/useCustomInputSubmissionStatus";
import EditorPanel from "./EditorPanel";
import ProblemPanel from "./ProblemPanel";
import ProblemSubmissionResultPanel, {verdictLabel} from "./ProblemSubmissionResultPanel";
import TestPanel, {type WorkspaceNotice} from "./TestPanel";
import type {SubmissionChip, WorkspacePanel} from "./TestPanelHeader";
import type {CustomInputDraft} from "./testCaseRows";
import WorkspaceLayout from "./WorkspaceLayout";
import WorkspaceStatusBar from "./WorkspaceStatusBar";
import WorkspaceToolbar from "./WorkspaceToolbar";
import type {CodingProblem} from "./codingProblemTypes";

type Props = {
  problem: CodingProblem;
};

const maxCustomTestCases = 20;
const fontSizes = [12, 14, 16, 18];

function readDraft(draftKey: string, starterCode: string) {
  try {
    return {source: localStorage.getItem(draftKey) ?? starterCode, available: true};
  } catch {
    return {source: starterCode, available: false};
  }
}

function readPanel(value: string | null): WorkspacePanel | null {
  return value === "tests" || value === "submission" ? value : null;
}

function describeSubmissionChip(status: ReturnType<typeof useProblemSubmissionStatus>): SubmissionChip {
  const submission = status.problemSubmission;

  if (submission?.status === "FINISHED") {
    return {label: verdictLabel(submission.verdict), tone: submission.verdict === "ACCEPTED" ? "passed" : "failed"};
  }

  if (submission || status.isLoading) {
    return {label: "Submission", tone: "pending"};
  }

  return {label: "Submission", tone: "none"};
}

const styles = stylex.create({
  workspace: {
    flexGrow: 1,
    minHeight: 0,
    display: "flex",
    flexDirection: "column"
  },
  editorColumn: {
    flexGrow: 1,
    minWidth: 0,
    minHeight: {
      default: 0,
      "@media (max-width: 800px)": 450
    },
    display: "flex",
    flexDirection: "column"
  },
  unavailableEditor: {
    flexGrow: 1,
    display: "grid",
    placeItems: "center",
    padding: 24,
    backgroundColor: "#2b2b2b",
    color: "#9da0a8"
  }
});

export default function CodingWorkspace(props: Props) {
  const {problem} = props;
  const configuration = problem.languageConfigurations[0];
  // Device-local drafts. Authenticated workspaces must also scope this key by viewer ID.
  const draftKey = configuration ? `interview-forge:draft:${problem.id}:${configuration.id}:v1` : null;
  const [draft, setDraft] = useState(() => (
    configuration && draftKey ? readDraft(draftKey, configuration.starterCode) : null
  ));
  const [problemOpen, setProblemOpen] = useState(true);
  const [testsOpen, setTestsOpen] = useState(true);
  const [fontSize, setFontSize] = useState(14);
  const [wordWrap, setWordWrap] = useState(false);
  const [cursor, setCursor] = useState<{line: number; column: number} | null>(null);

  const [searchParams, setSearchParams] = useSearchParams();
  const problemSubmissionId = searchParams.get("submission") || null;
  const customSubmissionId = searchParams.get("customSubmission") || null;
  const activePanel = readPanel(searchParams.get("panel"))
    ?? (customSubmissionId ? "tests" : problemSubmissionId ? "submission" : "tests");
  const [customInputs, setCustomInputs] = useState<CustomInputDraft[]>(() => {
    const examples = problem.examples.slice(0, maxCustomTestCases).map(example => ({
      id: example.id,
      inputJson: example.inputJson
    }));

    return examples.length > 0 ? examples : [{id: "first-input", inputJson: ""}];
  });

  const problemSubmissionStatus = useProblemSubmissionStatus(problemSubmissionId);
  const problemSubmissionRequest = useSubmitSolution(showProblemSubmission);
  const customSubmissionStatus = useCustomInputSubmissionStatus(customSubmissionId);
  const customSubmissionRequest = useEnqueueCustomInputSubmission(showCustomInputSubmission);
  const editorAvailable = Boolean(configuration && draft);
  const submitDisabled = !editorAvailable || problemSubmissionStatus.isUnresolved
    || problemSubmissionRequest.isSubmitting || problemSubmissionStatus.isPending;
  const runDisabled = !editorAvailable || customSubmissionStatus.isUnresolved
    || customSubmissionRequest.isEnqueuing || customSubmissionStatus.isPending;
  const inputErrors: Record<string, string> = {};

  customInputs.forEach((testCase, index) => {
    const error = customSubmissionRequest.fieldErrors[`cases[${index}].inputJson`];

    if (error) {
      inputErrors[testCase.id] = error;
    }
  });

  const notices: WorkspaceNotice[] = [];

  if (customSubmissionRequest.error) {
    notices.push({id: "tests", message: customSubmissionRequest.error, requiresSignIn: customSubmissionRequest.requiresSignIn});
  }

  if (problemSubmissionRequest.error) {
    notices.push({id: "submission", message: problemSubmissionRequest.error, requiresSignIn: problemSubmissionRequest.requiresSignIn});
  }

  function selectPanel(panel: WorkspacePanel) {
    setSearchParams(previous => {
      const updated = new URLSearchParams(previous);
      updated.set("panel", panel);
      return updated;
    }, {replace: true, preventScrollReset: true});
  }

  function showProblemSubmission(id: string) {
    setSearchParams(previous => {
      const updated = new URLSearchParams(previous);
      updated.set("submission", id);
      updated.set("panel", "submission");
      return updated;
    }, {replace: true, preventScrollReset: true});
  }

  function showCustomInputSubmission(id: string) {
    setSearchParams(previous => {
      const updated = new URLSearchParams(previous);
      updated.set("customSubmission", id);
      updated.set("panel", "tests");
      return updated;
    }, {replace: true, preventScrollReset: true});
  }

  function submitSolution() {
    if (!configuration || !draft || submitDisabled) {
      return;
    }

    setTestsOpen(true);
    problemSubmissionRequest.submitSolution(configuration.id, draft.source);
  }

  function runTests() {
    if (!configuration || !draft || runDisabled) {
      return;
    }

    setTestsOpen(true);
    selectPanel("tests");
    customSubmissionRequest.enqueueCustomInputSubmission(
      configuration.id,
      draft.source,
      customInputs.map(testCase => testCase.inputJson)
    );
  }

  function updateCustomInputs(cases: CustomInputDraft[]) {
    // Inputs stay fixed during admission; editing afterward clears errors tied to old positions.
    customSubmissionRequest.clearFeedback();
    setCustomInputs(cases);
  }

  function updateSource(source: string) {
    if (!draftKey) {
      return;
    }

    let available = true;

    try {
      localStorage.setItem(draftKey, source);
    } catch {
      available = false;
    }

    customSubmissionRequest.clearFeedback();
    setDraft({source, available});
  }

  return (
    <div sx={styles.workspace} role="main">
      <WorkspaceToolbar
        title={problem.title}
        editProblemUrl={problem.canEdit ? AppRoutes.EditProblem({slug: problem.slug}) : null}
        problemOpen={problemOpen}
        onToggleProblem={() => setProblemOpen(!problemOpen)}
        editorAvailable={editorAvailable}
        fontSize={fontSize}
        onCycleFontSize={() => setFontSize(fontSizes[(fontSizes.indexOf(fontSize) + 1) % fontSizes.length])}
        wordWrap={wordWrap}
        onToggleWordWrap={() => setWordWrap(!wordWrap)}
        resetDisabled={!configuration || !draft || draft.source === configuration.starterCode}
        onReset={() => configuration && updateSource(configuration.starterCode)}
        onRunTests={runTests}
        runDisabled={runDisabled}
        isEnqueuingTests={customSubmissionRequest.isEnqueuing}
        areTestsPending={customSubmissionStatus.isPending}
        onSubmit={submitSolution}
        submitDisabled={submitDisabled}
        isSubmitting={problemSubmissionRequest.isSubmitting}
        isPending={problemSubmissionStatus.isPending}
      />
      <WorkspaceLayout
        problem={problemOpen ? <ProblemPanel problem={problem} /> : null}
        editor={
          <div sx={styles.editorColumn}>
            {configuration && draft ? (
              <EditorPanel
                languageKey={configuration.language.key}
                languageName={configuration.language.displayName}
                source={draft.source}
                fontSize={fontSize}
                wordWrap={wordWrap}
                onSourceChange={updateSource}
                onCursorChange={(line, column) => setCursor({line, column})}
              />
            ) : (
              <div sx={styles.unavailableEditor} role="region" aria-label="Code editor">
                No starter code is available for this problem.
              </div>
            )}
            <TestPanel
              key={customSubmissionId ?? "not-run"}
              panel={activePanel}
              onSelectPanel={selectPanel}
              expanded={testsOpen}
              onToggleExpanded={() => setTestsOpen(!testsOpen)}
              cases={customInputs}
              maxCases={maxCustomTestCases}
              onCasesChange={updateCustomInputs}
              disabled={customSubmissionRequest.isEnqueuing}
              errors={inputErrors}
              submission={customSubmissionStatus.submission}
              isLoading={customSubmissionStatus.isLoading}
              error={customSubmissionStatus.error}
              onRetry={customSubmissionStatus.retryCustomInputSubmissionStatus}
              submissionChip={describeSubmissionChip(problemSubmissionStatus)}
              notices={notices}
            >
              <ProblemSubmissionResultPanel
                problemSubmission={problemSubmissionStatus.problemSubmission}
                isLoading={problemSubmissionStatus.isLoading}
                error={problemSubmissionStatus.error}
                onRetry={problemSubmissionStatus.retryProblemSubmissionStatus}
              />
            </TestPanel>
          </div>
        }
      />
      <WorkspaceStatusBar
        storageAvailable={draft?.available}
        cursor={cursor}
        languageName={configuration?.language.displayName}
      />
    </div>
  );
}

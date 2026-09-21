import * as stylex from "@stylexjs/stylex";
import {useId, useState} from "react";
import {useSearchParams} from "react-router";
import useSubmitSolution from "hooks/useSubmitSolution";
import useProblemSubmissionStatus from "hooks/useProblemSubmissionStatus";
import useEnqueueCustomInputSubmission from "hooks/useEnqueueCustomInputSubmission";
import useCustomInputSubmissionStatus from "hooks/useCustomInputSubmissionStatus";
import EditorPanel from "./EditorPanel";
import ProblemPanel from "./ProblemPanel";
import ProblemSubmissionActions from "./ProblemSubmissionActions";
import ProblemSubmissionResultPanel from "./ProblemSubmissionResultPanel";
import CustomInputEditor, {type CustomInputDraft} from "./CustomInputEditor";
import CustomInputSubmissionResultPanel from "./CustomInputSubmissionResultPanel";
import Control from "./WorkspaceControl";
import WorkspaceLayout from "./WorkspaceLayout";
import type {CodingProblem} from "./codingProblemTypes";

type Props = {
  problem: CodingProblem;
};

const workspacePanels = [
  {id: "inputs", label: "Test inputs"},
  {id: "results", label: "Test results"},
  {id: "submission", label: "Submission"}
] as const;

type WorkspacePanel = typeof workspacePanels[number]["id"];
const maxCustomTestCases = 20;

function readDraft(draftKey: string, starterCode: string) {
  try {
    return {source: localStorage.getItem(draftKey) ?? starterCode, available: true};
  } catch {
    return {source: starterCode, available: false};
  }
}

const styles = stylex.create({
  testPanel: {
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column",
    borderTop: "1px solid #43454a"
  },
  panelNavigation: {
    display: "flex",
    flexWrap: "wrap",
    gap: 6,
    padding: "8px 12px",
    borderBottom: "1px solid #393b40",
    flexShrink: 0
  },
  panelControl: {
    padding: "4px 10px",
    borderRadius: 4,
    color: "#9da0a8",
    fontSize: 12,
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  selectedPanel: {
    backgroundColor: "#2e436e",
    color: "#b5ceff"
  },
  panelBody: {
    display: "flex",
    flexDirection: "column",
    flex: 1,
    minHeight: 0
  },
  unavailableEditor: {
    display: "grid",
    placeItems: "center",
    padding: 24,
    backgroundColor: "#1e1f22",
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

  const [searchParams, setSearchParams] = useSearchParams();
  const problemSubmissionId = searchParams.get("submission") || null;
  const customSubmissionId = searchParams.get("customSubmission") || null;
  const panelId = useId();
  const selectedPanel = workspacePanels.find(panel => panel.id === searchParams.get("panel"));
  const activePanel = selectedPanel?.id ?? (customSubmissionId ? "results" : problemSubmissionId ? "submission" : "inputs");
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
  const submitDisabled = !configuration || !draft || problemSubmissionStatus.isUnresolved;
  const runDisabled = !configuration || !draft || customSubmissionStatus.isUnresolved;
  const inputErrors: Record<string, string> = {};

  customInputs.forEach((testCase, index) => {
    const error = customSubmissionRequest.fieldErrors[`cases[${index}].inputJson`];

    if (error) {
      inputErrors[testCase.id] = error;
    }
  });

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
      updated.set("panel", "results");
      return updated;
    }, {replace: true, preventScrollReset: true});
  }

  function submitSolution() {
    if (submitDisabled || problemSubmissionStatus.isPending || problemSubmissionRequest.isSubmitting) {
      return;
    }

    problemSubmissionRequest.submitSolution(configuration.id, draft.source);
  }

  function runTests() {
    if (runDisabled || customSubmissionStatus.isPending || customSubmissionRequest.isEnqueuing) {
      return;
    }

    selectPanel("inputs");
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
    <WorkspaceLayout
      problem={<ProblemPanel problem={problem} />}
      editor={configuration && draft ? (
        <EditorPanel
          languageKey={configuration.language.key}
          languageName={configuration.language.displayName}
          source={draft.source}
          starterCode={configuration.starterCode}
          onSourceChange={updateSource}
        />
      ) : (
        <div sx={styles.unavailableEditor} role="region" aria-label="Code editor">
          No starter code is available for this problem.
        </div>
      )}
      actions={
        <ProblemSubmissionActions
          storageAvailable={draft?.available}
          onSubmit={submitSolution}
          disabled={submitDisabled}
          isSubmitting={problemSubmissionRequest.isSubmitting}
          isPending={problemSubmissionStatus.isPending}
          error={problemSubmissionRequest.error}
          requiresSignIn={problemSubmissionRequest.requiresSignIn}
          onRunTests={runTests}
          runDisabled={runDisabled}
          isEnqueuingTests={customSubmissionRequest.isEnqueuing}
          areTestsPending={customSubmissionStatus.isPending}
          runError={customSubmissionRequest.error}
          runRequiresSignIn={customSubmissionRequest.requiresSignIn}
        />
      }
      results={
        <div sx={styles.testPanel}>
          <div sx={styles.panelNavigation} role="group" aria-label="Workspace panels">
            {workspacePanels.map(panel => (
              <Control
                key={panel.id}
                appearance={[styles.panelControl, activePanel === panel.id && styles.selectedPanel]}
                pressed={activePanel === panel.id}
                controls={panelId}
                onActivate={() => selectPanel(panel.id)}
              >
                {panel.label}
              </Control>
            ))}
          </div>

          <div id={panelId} sx={styles.panelBody}>
            {activePanel === "inputs" && (
              <CustomInputEditor
                cases={customInputs}
                maxCases={maxCustomTestCases}
                onChange={updateCustomInputs}
                disabled={customSubmissionRequest.isEnqueuing}
                errors={inputErrors}
              />
            )}
            {activePanel === "results" && (
              <CustomInputSubmissionResultPanel
                submission={customSubmissionStatus.submission}
                isLoading={customSubmissionStatus.isLoading}
                error={customSubmissionStatus.error}
                onRetry={customSubmissionStatus.retryCustomInputSubmissionStatus}
              />
            )}
            {activePanel === "submission" && (
              <ProblemSubmissionResultPanel
                problemSubmission={problemSubmissionStatus.problemSubmission}
                isLoading={problemSubmissionStatus.isLoading}
                error={problemSubmissionStatus.error}
                onRetry={problemSubmissionStatus.retryProblemSubmissionStatus}
              />
            )}
          </div>
        </div>
      }
    />
  );
}

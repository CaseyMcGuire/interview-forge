import * as stylex from "@stylexjs/stylex";
import {useId, useState} from "react";
import {useSearchParams} from "react-router";
import useSubmitSolution from "hooks/useSubmitSolution";
import useProblemSubmissionStatus from "hooks/useProblemSubmissionStatus";
import EditorPanel from "./EditorPanel";
import ProblemPanel from "./ProblemPanel";
import ProblemSubmissionActions from "./ProblemSubmissionActions";
import ProblemSubmissionResultPanel from "./ProblemSubmissionResultPanel";
import CustomInputEditor, {type CustomInputDraft} from "./CustomInputEditor";
import CustomInputSubmissionResultPanel from "./CustomInputSubmissionResultPanel";
import Control from "./WorkspaceControl";
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
  },
  workspace: {
    display: "grid",
    gridTemplateColumns: {
      default: "minmax(0, 0.43fr) minmax(0, 0.57fr)",
      "@media (max-width: 800px)": "minmax(0, 1fr)"
    },
    gridTemplateRows: {
      default: "minmax(0, 1fr) 254px",
      "@media (max-width: 800px)": "auto 450px auto auto"
    },
    flex: 1,
    minHeight: 0,
    margin: {
      default: "0 20px 20px",
      "@media (max-width: 600px)": "0 8px 8px"
    },
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#43454a",
    borderRadius: 12,
    overflow: "hidden",
    backgroundColor: "#2b2d30",
    boxShadow: "0 4px 20px #00000026"
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
  const panelId = useId();
  const [activePanel, setActivePanel] = useState<WorkspacePanel>(problemSubmissionId ? "submission" : "inputs");
  const [customInputs, setCustomInputs] = useState<CustomInputDraft[]>(() => {
    const examples = problem.examples.slice(0, maxCustomTestCases).map(example => ({
      id: example.id,
      inputJson: example.inputJson
    }));

    return examples.length > 0 ? examples : [{id: "first-input", inputJson: ""}];
  });

  const problemSubmissionStatus = useProblemSubmissionStatus(problemSubmissionId);
  const problemSubmissionRequest = useSubmitSolution(showProblemSubmission);
  const submitDisabled = !configuration || !draft || problemSubmissionStatus.isUnresolved;

  function showProblemSubmission(id: string) {
    setActivePanel("submission");
    setSearchParams(previous => {
      const updated = new URLSearchParams(previous);
      updated.set("submission", id);
      return updated;
    }, {replace: true, preventScrollReset: true});
  }

  function submitSolution() {
    if (submitDisabled || problemSubmissionStatus.isPending || problemSubmissionRequest.isSubmitting) {
      return;
    }

    problemSubmissionRequest.submitSolution(configuration.id, draft.source);
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

    setDraft({source, available});
  }

  return (
    <div sx={styles.workspace} role="main">
      <ProblemPanel problem={problem} />
      {configuration && draft ? (
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
      <ProblemSubmissionActions
        storageAvailable={draft?.available}
        onSubmit={submitSolution}
        disabled={submitDisabled}
        isSubmitting={problemSubmissionRequest.isSubmitting}
        isPending={problemSubmissionStatus.isPending}
        error={problemSubmissionRequest.error}
        requiresSignIn={problemSubmissionRequest.requiresSignIn}
      />
      <div sx={styles.testPanel}>
        <div sx={styles.panelNavigation} role="group" aria-label="Workspace panels">
          {workspacePanels.map(panel => (
            <Control
              key={panel.id}
              appearance={[styles.panelControl, activePanel === panel.id && styles.selectedPanel]}
              pressed={activePanel === panel.id}
              controls={panelId}
              onActivate={() => setActivePanel(panel.id)}
            >
              {panel.label}
            </Control>
          ))}
        </div>

        <div id={panelId} sx={styles.panelBody}>
          {activePanel === "inputs" && (
            <CustomInputEditor cases={customInputs} maxCases={maxCustomTestCases} onChange={setCustomInputs} />
          )}
          {activePanel === "results" && <CustomInputSubmissionResultPanel submission={null} />}
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
    </div>
  );
}

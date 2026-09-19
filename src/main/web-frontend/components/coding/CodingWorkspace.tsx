import * as stylex from "@stylexjs/stylex";
import {useState} from "react";
import {useSearchParams} from "react-router";
import useSubmitSolution from "hooks/useSubmitSolution";
import useSubmissionStatus from "hooks/useSubmissionStatus";
import EditorPanel from "./EditorPanel";
import ProblemPanel from "./ProblemPanel";
import SubmissionActions from "./SubmissionActions";
import SubmissionResultPanel from "./SubmissionResultPanel";
import type {CodingProblem} from "./codingProblemTypes";

type Props = {
  problem: CodingProblem;
};

function readDraft(draftKey: string, starterCode: string) {
  try {
    return {source: localStorage.getItem(draftKey) ?? starterCode, available: true};
  } catch {
    return {source: starterCode, available: false};
  }
}

const styles = stylex.create({
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
  const submissionId = searchParams.get("submission") || null;
  const submissionStatus = useSubmissionStatus(submissionId);
  const submissionRequest = useSubmitSolution(showSubmission);
  const submitDisabled = !configuration || !draft || submissionStatus.isUnresolved;

  function showSubmission(id: string) {
    setSearchParams(previous => {
      const updated = new URLSearchParams(previous);
      updated.set("submission", id);
      return updated;
    }, {replace: true, preventScrollReset: true});
  }

  function submitSolution() {
    if (submitDisabled || submissionStatus.isPending || submissionRequest.isSubmitting) {
      return;
    }

    submissionRequest.submitSolution(configuration.id, draft.source);
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
      <SubmissionActions
        storageAvailable={draft?.available}
        onSubmit={submitSolution}
        disabled={submitDisabled}
        isSubmitting={submissionRequest.isSubmitting}
        isPending={submissionStatus.isPending}
        error={submissionRequest.error}
        requiresSignIn={submissionRequest.requiresSignIn}
      />
      <SubmissionResultPanel
        submission={submissionStatus.submission}
        isLoading={submissionStatus.isLoading}
        error={submissionStatus.error}
        onRetry={submissionStatus.retrySubmissionStatus}
      />
    </div>
  );
}

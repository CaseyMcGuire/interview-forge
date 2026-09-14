import * as stylex from "@stylexjs/stylex";
import {useEffect, useState} from "react";
import EditorPanel from "./EditorPanel";
import ProblemPanel from "./ProblemPanel";
import SubmissionActions from "./SubmissionActions";
import TestResultsPanel from "./TestResultsPanel";
import WorkspaceHeader from "./WorkspaceHeader";
import type {CodingProblem} from "./codingProblemTypes";

function readDraft(draftKey: string, starterCode: string) {
  try {
    return {source: localStorage.getItem(draftKey) ?? starterCode, available: true};
  } catch {
    return {source: starterCode, available: false};
  }
}

const styles = stylex.create({
  page: {
    height: {
      default: "100dvh",
      "@media (max-width: 800px)": "auto"
    },
    minHeight: 640,
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#2b2d30",
    colorScheme: "dark",
    color: "#dfe1e5",
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 14,
    lineHeight: 1.6
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

export default function CodingWorkspace({problem}: {problem: CodingProblem}) {
  const configuration = problem.languageConfigurations[0];
  // Device-local drafts. Authenticated workspaces must also scope this key by viewer ID.
  const draftKey = configuration ? `interview-forge:draft:${problem.id}:${configuration.id}:v1` : null;
  const [draft, setDraft] = useState(() => (
    configuration && draftKey ? readDraft(draftKey, configuration.starterCode) : null
  ));

  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${problem.title} · Interview Forge`;
    return () => {document.title = previousTitle;};
  }, [problem.title]);

  function updateSource(source: string) {
    if (!draftKey) return;
    let available = true;
    try {
      localStorage.setItem(draftKey, source);
    } catch {
      available = false;
    }
    setDraft({source, available});
  }

  return (
    <div sx={styles.page}>
      <WorkspaceHeader />
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
        <SubmissionActions storageAvailable={draft?.available} />
        <TestResultsPanel examples={problem.examples} />
      </div>
    </div>
  );
}

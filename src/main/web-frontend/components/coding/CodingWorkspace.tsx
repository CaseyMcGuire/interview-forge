import * as stylex from "@stylexjs/stylex";
import {useEffect, useState} from "react";
import EditorPanel from "./EditorPanel";
import ProblemPanel from "./ProblemPanel";
import SubmissionActions from "./SubmissionActions";
import TestResultsPanel from "./TestResultsPanel";
import WorkspaceHeader from "./WorkspaceHeader";
import {sampleProblem as problem} from "./sampleProblem";

// This is a preview draft. Authenticated workspaces must also scope this key by viewer ID.
const draftKey = `interview-forge:preview-draft:${problem.slug}:kotlin:v1`;

function readDraft() {
  try {
    return {source: localStorage.getItem(draftKey) ?? problem.starterCode, available: true};
  } catch {
    return {source: problem.starterCode, available: false};
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

export default function CodingWorkspace() {
  const [draft, setDraft] = useState(readDraft);

  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${problem.title} · Interview Forge`;
    return () => {document.title = previousTitle;};
  }, []);

  function updateSource(source: string) {
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
        <ProblemPanel title={problem.title} examples={problem.examples} />
        <EditorPanel
          filename={problem.filename}
          source={draft.source}
          starterCode={problem.starterCode}
          onSourceChange={updateSource}
        />
        <SubmissionActions storageAvailable={draft.available} />
        <TestResultsPanel examples={problem.examples} />
      </div>
    </div>
  );
}

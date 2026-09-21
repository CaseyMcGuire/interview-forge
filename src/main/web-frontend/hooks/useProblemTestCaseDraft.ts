import {useState} from "react";
import type {ProblemTestCaseDraft} from "components/problems/ProblemTestCaseForm";
import useGenerateTestCaseExpectedOutput from "./useGenerateTestCaseExpectedOutput";

/** Keeps the answer paired with its input, discarding pending generation when the input or language changes. */
export default function useProblemTestCaseDraft(initialDraft: ProblemTestCaseDraft, initialLanguageId: string) {
  const [draft, setDraft] = useState(initialDraft);
  const [problemLanguageId, setProblemLanguageId] = useState(initialLanguageId);
  const generation = useGenerateTestCaseExpectedOutput(expectedOutputJson => {
    setDraft(currentDraft => ({...currentDraft, expectedOutputJson}));
  });

  function changeDraft(updated: ProblemTestCaseDraft) {
    const inputChanged = updated.inputJson !== draft.inputJson;
    if (inputChanged) {
      generation.resetGeneration();
    }

    setDraft(inputChanged ? {...updated, expectedOutputJson: ""} : updated);
  }

  function changeLanguage(id: string) {
    generation.resetGeneration();
    setProblemLanguageId(id);
    setDraft(currentDraft => ({...currentDraft, expectedOutputJson: ""}));
  }

  function generateExpectedOutput() {
    if (generation.isGenerating || !problemLanguageId || !draft.inputJson.trim()) {
      return;
    }

    setDraft(currentDraft => ({...currentDraft, expectedOutputJson: ""}));
    generation.generateExpectedOutput(problemLanguageId, draft.inputJson);
  }

  function resetDraft(updated: ProblemTestCaseDraft) {
    generation.resetGeneration();
    setDraft(updated);
  }

  return {draft, problemLanguageId, generation, changeDraft, changeLanguage, generateExpectedOutput, resetDraft};
}

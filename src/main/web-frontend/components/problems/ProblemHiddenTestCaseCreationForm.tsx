import {useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import type {ProblemHiddenTestCaseCreationFormMutation} from "__generated__/ProblemHiddenTestCaseCreationFormMutation.graphql";
import type {ProblemHiddenTestCaseCreationForm_problem$key} from "__generated__/ProblemHiddenTestCaseCreationForm_problem.graphql";
import useGenerateTestCaseExpectedOutput from "hooks/useGenerateTestCaseExpectedOutput";
import ProblemTestCaseForm, {type ProblemTestCaseDraft} from "./ProblemTestCaseForm";

type Props = {
  problem: ProblemHiddenTestCaseCreationForm_problem$key;
};

const emptyDraft: ProblemTestCaseDraft = {
  inputJson: "",
  expectedOutputJson: "",
  explanationMarkdown: ""
};

const fieldLabels: Record<string, string> = {
  problemId: "Problem",
  inputJson: "Input",
  expectedOutputJson: "Expected output",
  explanationMarkdown: "Explanation"
};

export default function ProblemHiddenTestCaseCreationForm(props: Props) {
  const problem = useFragment(graphql`
    fragment ProblemHiddenTestCaseCreationForm_problem on Problem {
      id
      languageConfigurations {
        id
        language {
          displayName
        }
      }
    }
  `, props.problem);

  const [draft, setDraft] = useState<ProblemTestCaseDraft>(emptyDraft);
  const [problemLanguageId, setProblemLanguageId] = useState(problem.languageConfigurations[0]?.id ?? "");
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);
  const generation = useGenerateTestCaseExpectedOutput(expectedOutputJson => {
    setDraft(currentDraft => ({...currentDraft, expectedOutputJson}));
  });

  const [commit, isSaving] = useMutation<ProblemHiddenTestCaseCreationFormMutation>(graphql`
    mutation ProblemHiddenTestCaseCreationFormMutation($input: CreateProblemHiddenTestCaseInput!) {
      createProblemHiddenTestCase(input: $input) {
        __typename
        ... on CreateProblemHiddenTestCaseSuccess {
          problem {
            id
          }
        }
        ... on ProblemValidationFailure {
          message
          fieldErrors {
            field
            message
          }
        }
        ... on ProblemNotFound {
          message
        }
        ... on ProblemForbidden {
          message
        }
      }
    }
  `);

  function save() {
    if (isSaving || generation.isGenerating || !generation.generated || !draft.inputJson.trim() || !draft.expectedOutputJson.trim()) {
      return;
    }

    setErrors([]);
    setSaved(false);

    commit({
      variables: {
        input: {
          problemId: problem.id,
          inputJson: draft.inputJson,
          expectedOutputJson: draft.expectedOutputJson,
          explanationMarkdown: draft.explanationMarkdown || null
        }
      },

      onCompleted: (response, graphqlErrors) => {
        const result = response.createProblemHiddenTestCase;

        if (graphqlErrors?.length || !result) {
          setErrors(["The hidden test case could not be added. Your input is still here; please try again."]);
          return;
        }

        switch (result.__typename) {
          case "CreateProblemHiddenTestCaseSuccess":
            generation.resetGeneration();
            setDraft(emptyDraft);
            setSaved(true);
            break;

          case "ProblemValidationFailure":
            setErrors(result.fieldErrors.length > 0
              ? result.fieldErrors.map((error) => (
                `${fieldLabels[error.field] ?? "Hidden test case"}: ${error.message}`
              ))
              : [result.message]);
            break;

          case "ProblemNotFound":
          case "ProblemForbidden":
            setErrors([result.message]);
            break;

          default:
            setErrors(["The hidden test case could not be added. Your input is still here; please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. Your input is still here; please try again."]);
      }
    });
  }

  function changeDraft(updated: ProblemTestCaseDraft) {
    const inputChanged = updated.inputJson !== draft.inputJson;
    if (inputChanged) {
      generation.resetGeneration();
    }

    setDraft(inputChanged ? {...updated, expectedOutputJson: ""} : updated);
    setErrors([]);
    setSaved(false);
  }

  function changeLanguage(id: string) {
    generation.resetGeneration();
    setProblemLanguageId(id);
    setDraft(currentDraft => ({...currentDraft, expectedOutputJson: ""}));
    setErrors([]);
    setSaved(false);
  }

  function generateExpectedOutput() {
    if (isSaving || generation.isGenerating || !problemLanguageId || !draft.inputJson.trim()) {
      return;
    }

    setErrors([]);
    setSaved(false);
    setDraft(currentDraft => ({...currentDraft, expectedOutputJson: ""}));
    generation.generateExpectedOutput(problemLanguageId, draft.inputJson);
  }

  return (
    <ProblemTestCaseForm
      mode="create"
      draft={draft}
      onChange={changeDraft}
      onSubmit={save}
      languages={problem.languageConfigurations.map(configuration => ({
        id: configuration.id,
        displayName: configuration.language.displayName
      }))}
      problemLanguageId={problemLanguageId}
      onLanguageChange={changeLanguage}
      onGenerateExpectedOutput={generateExpectedOutput}
      isGenerating={generation.isGenerating}
      generated={generation.generated}
      generationErrors={generation.errors}
      isSaving={isSaving}
      errors={errors}
      saved={saved}
    />
  );
}

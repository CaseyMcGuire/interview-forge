import {useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import type {ProblemTestCaseCreationFormMutation} from "__generated__/ProblemTestCaseCreationFormMutation.graphql";
import type {ProblemTestCaseCreationForm_problem$key} from "__generated__/ProblemTestCaseCreationForm_problem.graphql";
import useProblemTestCaseDraft from "hooks/useProblemTestCaseDraft";
import ProblemTestCaseForm, {type ProblemTestCaseDraft} from "./ProblemTestCaseForm";

type Props = {
  problem: ProblemTestCaseCreationForm_problem$key;
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

export default function ProblemTestCaseCreationForm(props: Props) {
  const problem = useFragment(graphql`
    fragment ProblemTestCaseCreationForm_problem on Problem {
      id
      languageConfigurations {
        id
        language {
          displayName
        }
      }
    }
  `, props.problem);

  const editor = useProblemTestCaseDraft(emptyDraft, problem.languageConfigurations[0]?.id ?? "");
  const {draft, problemLanguageId, generation} = editor;
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);

  const [commit, isSaving] = useMutation<ProblemTestCaseCreationFormMutation>(graphql`
    mutation ProblemTestCaseCreationFormMutation($input: CreateProblemHiddenTestCaseInput!) {
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
    if (isSaving || generation.isGenerating || !generation.generated ||
      !draft.inputJson.trim() || !draft.expectedOutputJson.trim()) {
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
          setErrors(["The test case could not be added. Your input is still here; please try again."]);
          return;
        }

        switch (result.__typename) {
          case "CreateProblemHiddenTestCaseSuccess":
            editor.resetDraft(emptyDraft);
            setSaved(true);
            break;

          case "ProblemValidationFailure":
            setErrors(result.fieldErrors.length > 0
              ? result.fieldErrors.map((error) => (
                `${fieldLabels[error.field] ?? "Test case"}: ${error.message}`
              ))
              : [result.message]);
            break;

          case "ProblemNotFound":
          case "ProblemForbidden":
            setErrors([result.message]);
            break;

          default:
            setErrors(["The test case could not be added. Your input is still here; please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. Your input is still here; please try again."]);
      }
    });
  }

  function clearFeedback() {
    setErrors([]);
    setSaved(false);
  }

  function changeDraft(updated: ProblemTestCaseDraft) {
    editor.changeDraft(updated);
    clearFeedback();
  }

  function changeLanguage(id: string) {
    editor.changeLanguage(id);
    clearFeedback();
  }

  function generateExpectedOutput() {
    if (isSaving) {
      return;
    }

    clearFeedback();
    editor.generateExpectedOutput();
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

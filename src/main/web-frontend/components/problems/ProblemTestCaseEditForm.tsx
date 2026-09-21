import {useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import type {ProblemTestCaseEditFormMutation} from "__generated__/ProblemTestCaseEditFormMutation.graphql";
import type {ProblemTestCaseEditForm_problem$key} from "__generated__/ProblemTestCaseEditForm_problem.graphql";
import type {ProblemTestCaseEditForm_testCase$key} from "__generated__/ProblemTestCaseEditForm_testCase.graphql";
import useProblemTestCaseDraft from "hooks/useProblemTestCaseDraft";
import ProblemTestCaseForm, {type ProblemTestCaseDraft} from "./ProblemTestCaseForm";

type Props = {
  problem: ProblemTestCaseEditForm_problem$key;
  testCase: ProblemTestCaseEditForm_testCase$key;
};

const fieldLabels: Record<string, string> = {
  inputJson: "Input",
  expectedOutputJson: "Expected output",
  explanationMarkdown: "Explanation"
};

export default function ProblemTestCaseEditForm(props: Props) {
  const problem = useFragment(graphql`
    fragment ProblemTestCaseEditForm_problem on Problem {
      languageConfigurations {
        id
        language {
          displayName
        }
      }
    }
  `, props.problem);
  const testCase = useFragment(graphql`
    fragment ProblemTestCaseEditForm_testCase on ProblemTestCase {
      id
      inputJson
      expectedOutputJson
      explanationMarkdown
    }
  `, props.testCase);

  const editor = useProblemTestCaseDraft({
    inputJson: testCase.inputJson,
    expectedOutputJson: testCase.expectedOutputJson,
    explanationMarkdown: testCase.explanationMarkdown ?? ""
  }, problem.languageConfigurations[0]?.id ?? "");
  const {draft, problemLanguageId, generation} = editor;
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);

  const [commit, isSaving] = useMutation<ProblemTestCaseEditFormMutation>(graphql`
    mutation ProblemTestCaseEditFormMutation($input: UpdateProblemTestCaseInput!) {
      updateProblemTestCase(input: $input) {
        __typename
        ... on UpdateProblemTestCaseSuccess {
          testCase {
            id
            inputJson
            expectedOutputJson
            explanationMarkdown
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
    if (isSaving || generation.isGenerating || !draft.inputJson.trim() || !draft.expectedOutputJson.trim()) {
      return;
    }

    clearFeedback();

    commit({
      variables: {
        input: {
          id: testCase.id,
          inputJson: draft.inputJson,
          expectedOutputJson: draft.expectedOutputJson,
          explanationMarkdown: draft.explanationMarkdown || null
        }
      },

      onCompleted: (response, graphqlErrors) => {
        const result = response.updateProblemTestCase;

        if (graphqlErrors?.length || !result) {
          setErrors(["The test case could not be saved. Your changes are still here; please try again."]);
          return;
        }

        switch (result.__typename) {
          case "UpdateProblemTestCaseSuccess":
            editor.resetDraft({
              inputJson: result.testCase.inputJson,
              expectedOutputJson: result.testCase.expectedOutputJson,
              explanationMarkdown: result.testCase.explanationMarkdown ?? ""
            });
            setSaved(true);
            break;

          case "ProblemValidationFailure":
            setErrors(result.fieldErrors.length > 0
              ? result.fieldErrors.map(error => `${fieldLabels[error.field] ?? "Test case"}: ${error.message}`)
              : [result.message]);
            break;

          case "ProblemNotFound":
          case "ProblemForbidden":
            setErrors([result.message]);
            break;

          default:
            setErrors(["The test case could not be saved. Your changes are still here; please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. Your changes are still here; please try again."]);
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
      mode="edit"
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

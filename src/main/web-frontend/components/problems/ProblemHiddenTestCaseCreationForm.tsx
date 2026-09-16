import {useState} from "react";
import {graphql, useMutation} from "react-relay";
import type {ProblemHiddenTestCaseCreationFormMutation} from "__generated__/ProblemHiddenTestCaseCreationFormMutation.graphql";
import ProblemHiddenTestCaseForm, {type ProblemHiddenTestCaseDraft} from "./ProblemHiddenTestCaseForm";

type Props = {
  problemId: string;
};

const emptyDraft: ProblemHiddenTestCaseDraft = {
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
  const [draft, setDraft] = useState<ProblemHiddenTestCaseDraft>(emptyDraft);
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);

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
    if (isSaving || !draft.inputJson.trim() || !draft.expectedOutputJson.trim()) {
      return;
    }

    setErrors([]);
    setSaved(false);

    commit({
      variables: {
        input: {
          problemId: props.problemId,
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

  function changeDraft(updated: ProblemHiddenTestCaseDraft) {
    setDraft(updated);
    setErrors([]);
    setSaved(false);
  }

  return (
    <ProblemHiddenTestCaseForm
      draft={draft}
      onChange={changeDraft}
      onSubmit={save}
      isSaving={isSaving}
      errors={errors}
      saved={saved}
    />
  );
}

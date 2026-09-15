import {useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemExampleEditForm_example$key} from "__generated__/ProblemExampleEditForm_example.graphql";
import type {
  ProblemExampleEditFormMutation,
  UpdateProblemExampleInput
} from "__generated__/ProblemExampleEditFormMutation.graphql";
import Control from "components/coding/WorkspaceControl";
import ProblemExampleEditor from "./ProblemExampleEditor";
import ProblemEditFeedback from "./ProblemEditFeedback";
import DeleteProblemExampleButton from "./DeleteProblemExampleButton";

type Props = {
  example: ProblemExampleEditForm_example$key;
  index: number;
};

const fieldLabels: Record<string, string> = {
  inputJson: "Input",
  expectedOutputJson: "Expected output",
  explanationMarkdown: "Explanation"
};

const styles = stylex.create({
  actions: {
    display: "flex",
    flexWrap: "wrap",
    gap: 12,
    paddingTop: 18,
    borderTop: "1px solid #393b40"
  },
  control: {
    padding: "10px 16px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    cursor: "pointer"
  },
  save: {
    backgroundColor: "#3574f0",
    borderColor: "#3574f0",
    color: "#ffffff"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

export default function ProblemExampleEditForm({example, index}: Props) {
  const data = useFragment(graphql`
    fragment ProblemExampleEditForm_example on ProblemExample {
      id
      inputJson
      expectedOutputJson
      explanationMarkdown
    }
  `, example);

  const [draft, setDraft] = useState({
    inputJson: data.inputJson,
    expectedOutputJson: data.expectedOutputJson,
    explanationMarkdown: data.explanationMarkdown ?? ""
  });
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);
  const [deleteActive, setDeleteActive] = useState(false);

  const [commitUpdate, isSaving] = useMutation<ProblemExampleEditFormMutation>(graphql`
    mutation ProblemExampleEditFormMutation($input: UpdateProblemExampleInput!) {
      updateProblemExample(input: $input) {
        __typename
        ... on UpdateProblemExampleSuccess {
          example {
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

  const hasChanges = draft.inputJson !== data.inputJson ||
    draft.expectedOutputJson !== data.expectedOutputJson ||
    draft.explanationMarkdown !== (data.explanationMarkdown ?? "");
  const saveDisabled = !hasChanges || isSaving || deleteActive;

  function save() {
    if (saveDisabled) {
      return;
    }

    const input: UpdateProblemExampleInput = {id: data.id};

    if (draft.inputJson !== data.inputJson) {
      input.inputJson = draft.inputJson;
    }

    if (draft.expectedOutputJson !== data.expectedOutputJson) {
      input.expectedOutputJson = draft.expectedOutputJson;
    }

    if (draft.explanationMarkdown !== (data.explanationMarkdown ?? "")) {
      input.explanationMarkdown = draft.explanationMarkdown || null;
    }

    setErrors([]);
    setSaved(false);

    commitUpdate({
      variables: {input},

      onCompleted: (response, graphqlErrors) => {
        const result = response.updateProblemExample;

        if (graphqlErrors?.length || !result) {
          setErrors(["The example could not be saved. Please try again."]);
          return;
        }

        switch (result.__typename) {
          case "UpdateProblemExampleSuccess":
            setDraft({
              inputJson: result.example.inputJson,
              expectedOutputJson: result.example.expectedOutputJson,
              explanationMarkdown: result.example.explanationMarkdown ?? ""
            });
            setSaved(true);
            break;

          case "ProblemValidationFailure":
            setErrors(result.fieldErrors.length > 0
              ? result.fieldErrors.map((error) => (
                `${fieldLabels[error.field] ?? "Example"}: ${error.message}`
              ))
              : [result.message]);
            break;

          case "ProblemNotFound":
          case "ProblemForbidden":
            setErrors([result.message]);
            break;

          default:
            setErrors(["The example could not be saved. Please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. Your changes are still here; please try again."]);
      },
    });
  }

  return (
    <ProblemExampleEditor
      example={draft}
      index={index}
      disabled={isSaving || deleteActive}
      canRemove={false}
      onChange={(updated) => setDraft({
        inputJson: updated.inputJson,
        expectedOutputJson: updated.expectedOutputJson,
        explanationMarkdown: updated.explanationMarkdown ?? ""
      })}
    >
      {!deleteActive && <ProblemEditFeedback errors={errors} saved={saved && !hasChanges} />}

      <div sx={styles.actions}>
        <Control
          appearance={[
            styles.control,
            styles.save,
            saveDisabled && styles.disabled
          ]}
          disabled={saveDisabled}
          onActivate={save}
        >
          {isSaving ? "Saving…" : "Save example"}
        </Control>

        <DeleteProblemExampleButton
          exampleId={data.id}
          index={index}
          disabled={isSaving}
          onActiveChange={setDeleteActive}
        />
      </div>
    </ProblemExampleEditor>
  );
}

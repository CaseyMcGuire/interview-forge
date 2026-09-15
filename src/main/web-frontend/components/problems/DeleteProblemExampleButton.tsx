import {useId, useState} from "react";
import {graphql, useMutation} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {DeleteProblemExampleButtonMutation} from "__generated__/DeleteProblemExampleButtonMutation.graphql";
import Control from "components/coding/WorkspaceControl";
import ProblemEditFeedback from "./ProblemEditFeedback";

type Props = {
  exampleId: string;
  index: number;
  disabled: boolean;
  onActiveChange: (active: boolean) => void;
};

const styles = stylex.create({
  control: {
    padding: "10px 16px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    cursor: "pointer"
  },
  delete: {
    color: "#f2a6a6"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  feedback: {
    flexBasis: "100%",
    display: "flex",
    flexDirection: "column",
    gap: 12
  },
  confirmation: {
    color: "#f2a6a6",
    lineHeight: 1.6
  }
});

export default function DeleteProblemExampleButton({
  exampleId,
  index,
  disabled,
  onActiveChange,
}: Props) {
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [errors, setErrors] = useState<readonly string[]>([]);
  const id = useId();

  const [commit, isDeleting] = useMutation<DeleteProblemExampleButtonMutation>(graphql`
    mutation DeleteProblemExampleButtonMutation($input: DeleteProblemExampleInput!) {
      deleteProblemExample(input: $input) {
        __typename
        ... on DeleteProblemExampleSuccess {
          deletedExampleId @deleteRecord
          problem {
            id
            examples {
              id
              ...ProblemExampleEditForm_example
            }
          }
        }
        ... on ProblemValidationFailure {
          message
          fieldErrors {
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

  const isDisabled = disabled || isDeleting;

  function deleteExample() {
    if (isDisabled || !confirmingDelete) {
      return;
    }

    setErrors([]);

    commit({
      variables: {
        input: {
          id: exampleId
        }
      },

      onCompleted: (response, graphqlErrors) => {
        const result = response.deleteProblemExample;

        if (graphqlErrors?.length || !result) {
          setErrors(["The example could not be deleted. Please try again."]);
          return;
        }

        switch (result.__typename) {
          case "DeleteProblemExampleSuccess":
            break;

          case "ProblemValidationFailure":
            setErrors(result.fieldErrors.length > 0
              ? result.fieldErrors.map((error) => error.message)
              : [result.message]);
            break;

          case "ProblemNotFound":
          case "ProblemForbidden":
            setErrors([result.message]);
            break;

          default:
            setErrors(["The example could not be deleted. Please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. The example is still shown; please try again."]);
      },
    });
  }

  function cancelDelete() {
    setConfirmingDelete(false);
    setErrors([]);
    onActiveChange(false);
  }

  return (
    <>
      <Control
        appearance={[
          styles.control,
          styles.delete,
          isDisabled && styles.disabled
        ]}
        disabled={isDisabled}
        description={confirmingDelete ? `${id}-delete-note` : undefined}
        onActivate={() => {
          if (confirmingDelete) {
            deleteExample();
          } else {
            setErrors([]);
            setConfirmingDelete(true);
            onActiveChange(true);
          }
        }}
      >
        {isDeleting ? "Deleting…" : confirmingDelete ? "Confirm delete" : "Delete example"}
      </Control>

      {confirmingDelete && (
        <>
          <Control
            appearance={[styles.control, isDisabled && styles.disabled]}
            disabled={isDisabled}
            onActivate={cancelDelete}
          >
            Cancel
          </Control>

          <div sx={styles.feedback}>
            <div id={`${id}-delete-note`} sx={styles.confirmation} role="alert">
              Delete example {index + 1}? This cannot be undone.
            </div>

            <ProblemEditFeedback errors={errors} saved={false} />
          </div>
        </>
      )}
    </>
  );
}

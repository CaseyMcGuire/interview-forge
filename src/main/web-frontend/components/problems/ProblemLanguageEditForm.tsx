import {useId, useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemLanguageEditForm_configuration$key} from "__generated__/ProblemLanguageEditForm_configuration.graphql";
import type {ProblemLanguageEditFormMutation} from "__generated__/ProblemLanguageEditFormMutation.graphql";
import CodeEditor from "components/coding/CodeEditor";
import Control from "components/coding/WorkspaceControl";
import ProblemEditFeedback from "./ProblemEditFeedback";

type Props = {
  configuration: ProblemLanguageEditForm_configuration$key;
};

const styles = stylex.create({
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 18,
    padding: 20,
    border: "1px solid #393b40",
    borderRadius: 8
  },
  heading: {
    fontWeight: 600
  },
  editor: {
    height: 280,
    overflow: "hidden",
    border: "1px solid #4e5157",
    borderRadius: 5
  },
  help: {
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.5
  },
  actions: {
    paddingTop: 18,
    borderTop: "1px solid #393b40"
  },
  save: {
    padding: "10px 16px",
    borderRadius: 5,
    width: "fit-content",
    backgroundColor: "#3574f0",
    color: "#ffffff",
    cursor: "pointer"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

export default function ProblemLanguageEditForm({configuration}: Props) {
  const data = useFragment(graphql`
    fragment ProblemLanguageEditForm_configuration on ProblemLanguage {
      id
      starterCode
      language {
        key
        displayName
      }
    }
  `, configuration);

  const [starterCode, setStarterCode] = useState(data.starterCode);
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);
  const id = useId();

  const [commit, isInFlight] = useMutation<ProblemLanguageEditFormMutation>(graphql`
    mutation ProblemLanguageEditFormMutation($input: UpdateProblemLanguageInput!) {
      updateProblemLanguage(input: $input) {
        __typename
        ... on UpdateProblemLanguageSuccess {
          problemLanguage {
            id
            starterCode
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

  const hasChanges = starterCode !== data.starterCode;
  const saveDisabled = !hasChanges || isInFlight;

  function save() {
    if (saveDisabled) {
      return;
    }

    setErrors([]);
    setSaved(false);

    commit({
      variables: {
        input: {
          id: data.id,
          starterCode
        }
      },

      onCompleted: (response, graphqlErrors) => {
        const result = response.updateProblemLanguage;

        if (graphqlErrors?.length || !result) {
          setErrors(["The starter code could not be saved. Please try again."]);
          return;
        }

        switch (result.__typename) {
          case "UpdateProblemLanguageSuccess":
            setStarterCode(result.problemLanguage.starterCode);
            setSaved(true);
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
            setErrors(["The starter code could not be saved. Please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. Your changes are still here; please try again."]);
      },
    });
  }

  return (
    <div sx={styles.form} aria-busy={isInFlight}>
      <div sx={styles.heading} role="heading" aria-level={3}>
        {data.language.displayName}
      </div>

      <span>Starter code</span>

      <div sx={styles.editor} inert={isInFlight}>
        <CodeEditor
          languageKey={data.language.key}
          label={`${data.language.displayName} starter code`}
          describedBy={`${id}-help`}
          value={starterCode}
          onChange={setStarterCode}
          fontSize={14}
          wordWrap
          onCursorChange={() => {}}
        />
      </div>

      <div id={`${id}-help`} sx={styles.help}>
        Tab indents code. Press Escape, then Tab to leave the editor.
      </div>

      <div sx={styles.actions}>
        <Control
          appearance={[styles.save, saveDisabled && styles.disabled]}
          disabled={saveDisabled}
          onActivate={save}
        >
          {isInFlight ? "Saving…" : "Save starter code"}
        </Control>
      </div>

      <ProblemEditFeedback errors={errors} saved={saved && !hasChanges} />
    </div>
  );
}

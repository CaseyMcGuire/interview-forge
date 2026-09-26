import {useId, useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemDetailsForm_problem$key} from "__generated__/ProblemDetailsForm_problem.graphql";
import type {ProblemTagField_query$key} from "__generated__/ProblemTagField_query.graphql";
import type {
  ProblemDetailsFormMutation,
  UpdateProblemInput
} from "__generated__/ProblemDetailsFormMutation.graphql";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";
import ProblemEditFeedback from "./ProblemEditFeedback";
import ProblemTagField from "./ProblemTagField";

type Props = {
  problem: ProblemDetailsForm_problem$key;
  tagCatalog: ProblemTagField_query$key;
};

const difficulties = [
  {value: "EASY", label: "Easy"},
  {value: "MEDIUM", label: "Medium"},
  {value: "HARD", label: "Hard"}
] as const;

const styles = stylex.create({
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 24
  },
  actions: {
    paddingTop: 20,
    borderTop: "1px solid #393b40"
  },
  choices: {
    display: "flex",
    gap: 8,
    marginTop: 10
  },
  control: {
    padding: "10px 16px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    cursor: "pointer",
    width: "fit-content",
    backgroundColor: "#2b2d30"
  },
  selected: {
    backgroundColor: "#253754",
    borderColor: "#6b9bfa"
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

export default function ProblemDetailsForm(props: Props) {
  const data = useFragment(graphql`
    fragment ProblemDetailsForm_problem on Problem {
      id
      title
      statementMarkdown
      difficulty
      tags {
        id
      }
    }
  `, props.problem);

  const [title, setTitle] = useState(data.title);
  const [statementMarkdown, setStatementMarkdown] = useState(data.statementMarkdown);
  const [difficulty, setDifficulty] = useState(data.difficulty);
  const [tagIds, setTagIds] = useState(() => data.tags.map((tag) => tag.id));
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);
  const id = useId();

  const [commit, isInFlight] = useMutation<ProblemDetailsFormMutation>(graphql`
    mutation ProblemDetailsFormMutation($input: UpdateProblemInput!) {
      updateProblem(input: $input) {
        __typename
        ... on UpdateProblemSuccess {
          problem {
            id
            title
            statementMarkdown
            difficulty
            tags {
              id
              displayName
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

  const tagsChanged = tagIds.length !== data.tags.length ||
    data.tags.some((tag) => !tagIds.includes(tag.id));
  const hasChanges = title !== data.title ||
    statementMarkdown !== data.statementMarkdown ||
    difficulty !== data.difficulty ||
    tagsChanged;
  const saveDisabled = !hasChanges || isInFlight;

  function save() {
    if (saveDisabled) {
      return;
    }

    const input: UpdateProblemInput = {id: data.id};

    if (title !== data.title) {
      input.title = title;
    }

    if (statementMarkdown !== data.statementMarkdown) {
      input.statementMarkdown = statementMarkdown;
    }

    if (difficulty !== data.difficulty) {
      input.difficulty = difficulty;
    }

    if (tagsChanged) {
      input.tagIds = tagIds;
    }

    setErrors([]);
    setSaved(false);

    commit({
      variables: {input},

      onCompleted: (response, graphqlErrors) => {
        const result = response.updateProblem;

        if (graphqlErrors?.length || !result) {
          setErrors(["The problem could not be saved. Please try again."]);
          return;
        }

        switch (result.__typename) {
          case "UpdateProblemSuccess":
            setTitle(result.problem.title);
            setStatementMarkdown(result.problem.statementMarkdown);
            setDifficulty(result.problem.difficulty);
            setTagIds(result.problem.tags.map((tag) => tag.id));
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
            setErrors(["The problem could not be saved. Please try again."]);
        }
      },

      onError: () => {
        setErrors(["The request failed. Your changes are still here; please try again."]);
      },
    });
  }

  return (
    <div sx={styles.form} aria-busy={isInFlight}>
      <ProblemCreationField
        label="Title"
        value={title}
        onChange={setTitle}
        maxLength={200}
        disabled={isInFlight}
      />

      <div>
        <span id={`${id}-difficulty`}>Difficulty</span>

        <div sx={styles.choices} role="group" aria-labelledby={`${id}-difficulty`}>
          {difficulties.map((option) => (
            <Control
              key={option.value}
              appearance={[
                styles.control,
                difficulty === option.value && styles.selected
              ]}
              pressed={difficulty === option.value}
              disabled={isInFlight}
              onActivate={() => setDifficulty(option.value)}
            >
              {option.label}
            </Control>
          ))}
        </div>
      </div>

      <ProblemTagField
        query={props.tagCatalog}
        selectedTagIds={tagIds}
        disabled={isInFlight}
        onChange={setTagIds}
      />

      <ProblemCreationField
        label="Statement (Markdown)"
        value={statementMarkdown}
        onChange={setStatementMarkdown}
        rows={9}
        maxLength={100_000}
        disabled={isInFlight}
      />

      <ProblemEditFeedback errors={errors} saved={saved && !hasChanges} />

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
          {isInFlight ? "Saving…" : "Save details"}
        </Control>
      </div>
    </div>
  );
}

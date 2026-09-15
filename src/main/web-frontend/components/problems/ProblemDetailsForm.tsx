import {useId, useState} from "react";
import {graphql, useFragment} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemDetailsForm_problem$key} from "__generated__/ProblemDetailsForm_problem.graphql";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";

type Props = {
  problem: ProblemDetailsForm_problem$key;
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
    color: "#ffffff",
    opacity: 0.6,
    cursor: "default"
  }
});

export default function ProblemDetailsForm({problem}: Props) {
  const data = useFragment(graphql`
    fragment ProblemDetailsForm_problem on Problem {
      id
      title
      statementMarkdown
      difficulty
    }
  `, problem);

  const [title, setTitle] = useState(data.title);
  const [statementMarkdown, setStatementMarkdown] = useState(data.statementMarkdown);
  const [difficulty, setDifficulty] = useState(data.difficulty);
  const id = useId();

  return (
    <div sx={styles.form}>
      <ProblemCreationField
        label="Title"
        value={title}
        onChange={setTitle}
        maxLength={200}
        disabled={false}
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
              onActivate={() => setDifficulty(option.value)}
            >
              {option.label}
            </Control>
          ))}
        </div>
      </div>

      <ProblemCreationField
        label="Statement (Markdown)"
        value={statementMarkdown}
        onChange={setStatementMarkdown}
        rows={9}
        maxLength={100_000}
        disabled={false}
      />

      <div sx={styles.actions}>
        <Control appearance={[styles.control, styles.save]} disabled>
          Save details
        </Control>
      </div>
    </div>
  );
}

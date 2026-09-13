import {useRef, useState} from "react";
import {useNavigate} from "react-router";
import {graphql, useMutation} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {EditProblemPageQuery$data} from "__generated__/EditProblemPageQuery.graphql";
import type {ProblemDifficulty, ProblemEditFormMutation, UpdateProblemExampleInput} from "__generated__/ProblemEditFormMutation.graphql";
import {AppRoutes} from "routes/AppRoutes";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";
import ProblemExampleEditor from "./ProblemExampleEditor";
import ProblemLanguageEditor from "./ProblemLanguageEditor";

type EditableProblem = NonNullable<EditProblemPageQuery$data["problem"]>;

const difficulties: {value: ProblemDifficulty; label: string}[] = [
  {value: "EASY", label: "Easy"},
  {value: "MEDIUM", label: "Medium"},
  {value: "HARD", label: "Hard"}
];

const styles = stylex.create({
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 24
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
  heading: {
    fontSize: 18,
    fontWeight: 600,
    marginBottom: 12
  },
  editors: {
    display: "flex",
    flexDirection: "column",
    gap: 16
  },
  actions: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    gap: 12
  },
  save: {
    backgroundColor: "#3574f0",
    borderColor: "#3574f0",
    color: "#ffffff",
    fontWeight: 600
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  error: {
    color: "#f2a6a6",
    lineHeight: 1.6
  }
});

export default function ProblemEditForm({problem}: {problem: EditableProblem}) {
  const navigate = useNavigate();

  const [title, setTitle] = useState(problem.title);
  const [statementMarkdown, setStatementMarkdown] = useState(problem.statementMarkdown);
  const [difficulty, setDifficulty] = useState(problem.difficulty);

  const [languageConfigurations, setLanguageConfigurations] = useState(() => (
    problem.languageConfigurations.map((configuration) => ({
      id: configuration.id,
      languageKey: configuration.language.key,
      starterCode: configuration.starterCode,
      solutionFilename: configuration.solutionFilename
    }))
  ));

  const [examples, setExamples] = useState<UpdateProblemExampleInput[]>(() => (
    problem.examples.map((example) => ({
      id: example.id,
      inputJson: example.inputJson,
      expectedOutputJson: example.expectedOutputJson,
      explanationMarkdown: example.explanationMarkdown
    }))
  ));

  const [error, setError] = useState<string | null>(null);
  const submitting = useRef(false);

  const [commit, isInFlight] = useMutation<ProblemEditFormMutation>(graphql`
    mutation ProblemEditFormMutation($slug: String!, $input: UpdateProblemInput!) {
      updateProblem(slug: $slug, input: $input) {
        __typename
        ... on UpdateProblemSuccess {
          problem {
            id
            slug
            title
            statementMarkdown
            difficulty
            languageConfigurations {
              id
              starterCode
              solutionFilename
              language {
                id
                key
                displayName
              }
            }
            examples {
              id
              position
              inputJson
              expectedOutputJson
              explanationMarkdown
            }
          }
        }
        ... on UpdateProblemFailure {
          message
        }
      }
    }
  `);

  function save() {
    if (submitting.current) {
      return;
    }

    if (!difficulties.some(({value}) => value === difficulty)) {
      setError("Choose a supported difficulty before saving.");
      return;
    }

    setError(null);
    submitting.current = true;

    commit({
      variables: {
        slug: problem.slug,
        input: {
          title,
          statementMarkdown,
          difficulty,
          languageConfigurations: languageConfigurations.map(({id, starterCode, solutionFilename}) => ({
            id,
            starterCode,
            solutionFilename
          })),
          examples
        }
      },

      onCompleted: (data, errors) => {
        submitting.current = false;

        if (errors?.length || !data.updateProblem) {
          setError(errors?.[0]?.message ?? "The request failed. Your edits are still here; please try again.");
          return;
        }

        const result = data.updateProblem;
        if (result.problem) {
          navigate(AppRoutes.Problem({slug: result.problem.slug}));
        } else {
          setError(result.message ?? "The server returned an unexpected result. Your edits are still here.");
        }
      },

      onError: () => {
        submitting.current = false;
        setError("The request failed. Your edits are still here; please try again.");
      },
    });
  }

  const languages = problem.languageConfigurations.map(({language}) => language);

  return (
    <div sx={styles.form} aria-busy={isInFlight}>
      <ProblemCreationField
        label="Title"
        value={title}
        onChange={setTitle}
        maxLength={200}
        disabled={isInFlight}
      />

      <ProblemCreationField
        label="URL slug"
        value={problem.slug}
        onChange={() => {}}
        hint="The slug cannot be changed."
        maxLength={100}
        disabled
      />

      <div>
        <span id="difficulty-label">Difficulty</span>

        <div sx={styles.choices} role="group" aria-labelledby="difficulty-label">
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

      <ProblemCreationField
        label="Statement (Markdown)"
        value={statementMarkdown}
        onChange={setStatementMarkdown}
        rows={9}
        maxLength={100_000}
        disabled={isInFlight}
      />

      <div>
        <div sx={styles.heading} role="heading" aria-level={2}>
          Language configurations
        </div>

        <div sx={styles.editors}>
          {languageConfigurations.map((configuration) => (
            <ProblemLanguageEditor
              key={configuration.id}
              configuration={configuration}
              languages={languages}
              disabled={isInFlight}
              languageLocked
              canRemove={false}
              onRemove={() => {}}
              onChange={(updated) => setLanguageConfigurations(
                languageConfigurations.map((item) => (
                  item.id === configuration.id ? {...item, ...updated} : item
                ))
              )}
            />
          ))}
        </div>
      </div>

      <div>
        <div sx={styles.heading} role="heading" aria-level={2}>
          Public examples
        </div>

        <div sx={styles.editors}>
          {examples.map((example, index) => (
            <ProblemExampleEditor
              key={example.id}
              index={index}
              example={example}
              disabled={isInFlight}
              canRemove={false}
              onRemove={() => {}}
              onChange={(updated) => setExamples(
                examples.map((item) => item.id === example.id ? {...item, ...updated} : item)
              )}
            />
          ))}
        </div>
      </div>

      {error && <div role="alert" sx={styles.error}>{error}</div>}

      <div sx={styles.actions}>
        <Control
          appearance={[
            styles.control,
            styles.save,
            isInFlight && styles.disabled
          ]}
          disabled={isInFlight}
          onActivate={save}
        >
          {isInFlight ? "Saving…" : "Save changes"}
        </Control>

        <Control
          appearance={styles.control}
          disabled={isInFlight}
          onActivate={() => navigate(AppRoutes.Problem({slug: problem.slug}))}
        >
          Cancel
        </Control>
      </div>
    </div>
  );
}

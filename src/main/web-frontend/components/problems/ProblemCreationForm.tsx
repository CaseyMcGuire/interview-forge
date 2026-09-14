import {useRef, useState} from "react";
import * as stylex from "@stylexjs/stylex";
import {graphql, useLazyLoadQuery, useMutation} from "react-relay";
import {useNavigate} from "react-router";
import type {
  CreateProblemExampleInput,
  CreateProblemLanguageInput,
  ProblemCreationFormMutation,
  ProblemDifficulty
} from "__generated__/ProblemCreationFormMutation.graphql";
import type {ProblemCreationFormQuery} from "__generated__/ProblemCreationFormQuery.graphql";
import {AppRoutes} from "routes/AppRoutes";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";
import ProblemExampleEditor from "./ProblemExampleEditor";
import ProblemLanguageEditor from "./ProblemLanguageEditor";

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

  examples: {
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

  publish: {
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

export default function ProblemCreationForm() {
  const {languages} = useLazyLoadQuery<ProblemCreationFormQuery>(graphql`
    query ProblemCreationFormQuery @throwOnFieldError {
      languages {
        id
        key
        displayName
      }
    }
  `, {});

  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [statementMarkdown, setStatementMarkdown] = useState("");
  const [difficulty, setDifficulty] = useState<ProblemDifficulty>("EASY");

  const [languageConfigurations, setLanguageConfigurations] = useState<CreateProblemLanguageInput[]>(() => (
    languages.length > 0
      ? [
        {
          languageKey: languages[0].key,
          starterCode: ""
        }
      ]
      : []
  ));

  const [examples, setExamples] = useState<CreateProblemExampleInput[]>([
    {
      inputJson: "",
      expectedOutputJson: "",
      explanationMarkdown: ""
    }
  ]);

  const [error, setError] = useState<string | null>(null);
  const submitting = useRef(false);

  const [commit, isInFlight] = useMutation<ProblemCreationFormMutation>(graphql`
    mutation ProblemCreationFormMutation($input: CreateProblemInput!) {
      createProblem(input: $input) {
        id
        slug
      }
    }
  `);

  function publish() {
    if (submitting.current) return;

    setError(null);
    submitting.current = true;

    commit({
      variables: {
        input: {
          title,
          slug,
          statementMarkdown,
          difficulty,
          languageConfigurations,
          examples
        }
      },

      onCompleted: (data, errors) => {
        submitting.current = false;

        if (errors?.length || !data.createProblem) {
          setError(errors?.[0]?.message ?? "The problem could not be created. Please try again.");
          return;
        }

        navigate(AppRoutes.Problem({slug: data.createProblem.slug}));
      },

      onError: () => {
        submitting.current = false;
        setError("The request failed. Your inputs are still here; please try again.");
      },
    });
  }

  if (languages.length === 0) {
    return (
      <div role="status">
        No languages are enabled. Enable a language in the catalog before creating a problem.
      </div>
    );
  }

  const unusedLanguages = languages.filter((language) => (
    !languageConfigurations.some((configuration) => configuration.languageKey === language.key)
  ));

  const nextLanguage = unusedLanguages[0];

  function addLanguage() {
    if (!nextLanguage) return;

    setLanguageConfigurations([
      ...languageConfigurations,
      {
        languageKey: nextLanguage.key,
        starterCode: ""
      }
    ]);
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

      <ProblemCreationField
        label="URL slug"
        value={slug}
        onChange={setSlug}
        hint="Use lowercase letters, numbers, and hyphens, such as two-sum. The slug cannot be changed later."
        maxLength={100}
        disabled={isInFlight}
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
        hint="Describe the task, requirements, and constraints. Add examples below."
        rows={9}
        maxLength={100_000}
        disabled={isInFlight}
      />

      <div>
        <div sx={styles.heading} role="heading" aria-level={2}>
          Language configurations
        </div>

        <div sx={styles.examples}>
          {languageConfigurations.map((configuration, index) => (
            <ProblemLanguageEditor
              key={configuration.languageKey}
              configuration={configuration}
              languages={languages.filter((language) => (
                language.key === configuration.languageKey ||
                unusedLanguages.some((unused) => unused.key === language.key)
              ))}
              disabled={isInFlight}
              canRemove={languageConfigurations.length > 1}
              onChange={(updated) => setLanguageConfigurations(
                languageConfigurations.map((item, position) => (
                  position === index ? updated : item
                ))
              )}
              onRemove={() => setLanguageConfigurations(
                languageConfigurations.filter((_, position) => position !== index)
              )}
            />
          ))}

          <Control
            appearance={styles.control}
            disabled={isInFlight || !nextLanguage || languageConfigurations.length >= 20}
            onActivate={addLanguage}
          >
            Add language
          </Control>
        </div>
      </div>

      <div>
        <div sx={styles.heading} role="heading" aria-level={2}>
          Public examples
        </div>

        <div sx={styles.examples}>
          {examples.map((example, index) => (
            <ProblemExampleEditor
              key={index}
              index={index}
              example={example}
              disabled={isInFlight}
              canRemove={examples.length > 1}
              onChange={(updated) => setExamples(
                examples.map((item, position) => position === index ? updated : item)
              )}
              onRemove={() => setExamples(
                examples.filter((_, position) => position !== index)
              )}
            />
          ))}

          <Control
            appearance={styles.control}
            disabled={isInFlight || examples.length >= 20}
            onActivate={() => setExamples([
              ...examples,
              {
                inputJson: "",
                expectedOutputJson: "",
                explanationMarkdown: ""
              }
            ])}
          >
            Add example
          </Control>
        </div>
      </div>

      {error && <div role="alert" sx={styles.error}>{error}</div>}

      <div sx={styles.actions}>
        <Control
          appearance={[
            styles.control,
            styles.publish,
            isInFlight && styles.disabled
          ]}
          disabled={isInFlight}
          onActivate={publish}
        >
          {isInFlight ? "Publishing…" : "Create and publish"}
        </Control>

        <Control
          appearance={styles.control}
          disabled={isInFlight}
          onActivate={() => navigate(AppRoutes.Problems())}
        >
          Cancel
        </Control>
      </div>
    </div>
  );
}

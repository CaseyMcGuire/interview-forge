import {useId} from "react";
import * as stylex from "@stylexjs/stylex";
import type {CreateProblemLanguageInput} from "__generated__/ProblemCreationFormMutation.graphql";
import type {ProblemCreationFormQuery$data} from "__generated__/ProblemCreationFormQuery.graphql";
import CodeEditor from "components/coding/CodeEditor";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";

type Props = {
  configuration: CreateProblemLanguageInput;
  languages: ProblemCreationFormQuery$data["languages"];
  disabled: boolean;
  languageLocked?: boolean;
  canRemove: boolean;
  onChange: (configuration: CreateProblemLanguageInput) => void;
  onRemove: () => void;
};

const styles = stylex.create({
  configuration: {
    display: "flex",
    flexDirection: "column",
    gap: 18,
    padding: 20,
    border: "1px solid #393b40",
    borderRadius: 8
  },

  heading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12
  },

  language: {
    display: "flex",
    alignItems: "center",
    gap: 12
  },

  select: {
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    border: "1px solid #4e5157",
    borderRadius: 5,
    padding: "8px 12px",
    fontSize: 14
  },

  remove: {
    color: "#9da0a8",
    cursor: "pointer",
    padding: 6,
    borderRadius: 4
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
  }
});

export default function ProblemLanguageEditor({
  configuration,
  languages,
  disabled,
  languageLocked = false,
  canRemove,
  onChange,
  onRemove,
}: Props) {
  const id = useId();

  const languageName = languages.find(
    (language) => language.key === configuration.languageKey
  )?.displayName ?? "Language";

  return (
    <div sx={styles.configuration}>
      <div sx={styles.heading}>
        <div sx={styles.language}>
          <span id={`${id}-language`}>Language</span>

          <select
            sx={styles.select}
            aria-labelledby={`${id}-language`}
            value={configuration.languageKey}
            disabled={disabled || languageLocked}
            onChange={(event) => onChange({...configuration, languageKey: event.target.value})}
          >
            {languages.map((language) => (
              <option key={language.key} value={language.key}>
                {language.displayName}
              </option>
            ))}
          </select>
        </div>

        {canRemove && (
          <Control
            appearance={styles.remove}
            disabled={disabled}
            onActivate={onRemove}
            label={`Remove ${languageName} configuration`}
          >
            Remove
          </Control>
        )}
      </div>

      <ProblemCreationField
        label={`${languageName} solution filename`}
        value={configuration.solutionFilename}
        onChange={(solutionFilename) => onChange({...configuration, solutionFilename})}
        maxLength={255}
        disabled={disabled}
      />

      <span>Starter code</span>

      <div sx={styles.editor} inert={disabled}>
        <CodeEditor
          languageKey={configuration.languageKey}
          label={`${languageName} starter code`}
          describedBy={`${id}-help`}
          value={configuration.starterCode}
          onChange={(starterCode) => onChange({...configuration, starterCode})}
          fontSize={14}
          wordWrap
          onCursorChange={() => {}}
        />
      </div>

      <div id={`${id}-help`} sx={styles.help}>
        Tab indents code. Press Escape, then Tab to leave the editor.
      </div>
    </div>
  );
}

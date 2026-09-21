import {useId} from "react";
import * as stylex from "@stylexjs/stylex";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";
import ProblemEditFeedback from "./ProblemEditFeedback";

export type ProblemHiddenTestCaseDraft = {
  inputJson: string;
  expectedOutputJson: string;
  explanationMarkdown: string;
};

type Props = {
  draft: ProblemHiddenTestCaseDraft;
  onChange: (draft: ProblemHiddenTestCaseDraft) => void;
  onSubmit: () => void;
  languages: readonly {id: string; displayName: string}[];
  problemLanguageId: string;
  onLanguageChange: (id: string) => void;
  onGenerateExpectedOutput: () => void;
  isGenerating: boolean;
  generated: boolean;
  generationErrors: readonly string[];
  isSaving: boolean;
  errors: readonly string[];
  saved: boolean;
};

const styles = stylex.create({
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 18
  },
  description: {
    color: "#9da0a8",
    lineHeight: 1.6
  },
  generation: {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "flex-end",
    gap: 12
  },
  language: {
    display: "flex",
    flexDirection: "column",
    gap: 8
  },
  label: {
    fontWeight: 600
  },
  select: {
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    border: "1px solid #4e5157",
    borderRadius: 5,
    padding: "10px 12px",
    fontSize: 14
  },
  generate: {
    borderColor: "#4e5157",
    backgroundColor: "#2b2d30",
    color: "#dfe1e5"
  },
  actions: {
    paddingTop: 18,
    borderTop: "1px solid #393b40"
  },
  control: {
    padding: "10px 16px",
    border: "1px solid #3574f0",
    borderRadius: 5,
    backgroundColor: "#3574f0",
    color: "#ffffff",
    cursor: "pointer",
    width: "fit-content"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  error: {
    color: "#f2a6a6",
    lineHeight: 1.6
  },
  status: {
    color: "#9da0a8"
  }
});

export default function ProblemHiddenTestCaseForm(props: Props) {
  const {draft, onChange, onSubmit, isSaving, errors, saved} = props;
  const languageSelectId = useId();
  const submitDisabled = isSaving || props.isGenerating || !props.generated ||
    !draft.inputJson.trim() ||
    !draft.expectedOutputJson.trim();
  const generateDisabled = isSaving || props.isGenerating ||
    !props.problemLanguageId || !draft.inputJson.trim();

  return (
    <div sx={styles.form} aria-busy={isSaving || props.isGenerating}>
      <div sx={styles.description}>
        Hidden test cases are not shown to people solving the problem.
      </div>

      <ProblemCreationField
        label="Hidden test input (JSON)"
        value={draft.inputJson}
        onChange={(inputJson) => onChange({...draft, inputJson})}
        rows={4}
        maxLength={20_000}
        disabled={isSaving}
      />

      <div sx={styles.generation}>
        <div sx={styles.language}>
          <label htmlFor={languageSelectId} sx={styles.label}>Reference solution language</label>
          <select
            id={languageSelectId}
            sx={styles.select}
            value={props.problemLanguageId}
            disabled={isSaving || props.languages.length === 0}
            onChange={event => props.onLanguageChange(event.target.value)}
          >
            {props.languages.length === 0 && <option value="">No languages configured</option>}
            {props.languages.map(language => (
              <option key={language.id} value={language.id}>{language.displayName}</option>
            ))}
          </select>
        </div>

        <Control
          appearance={[styles.control, styles.generate, generateDisabled && styles.disabled]}
          disabled={generateDisabled}
          onActivate={props.onGenerateExpectedOutput}
        >
          {props.isGenerating ? "Generating…" : "Generate expected output"}
        </Control>
      </div>

      <ProblemEditFeedback errors={props.generationErrors} saved={false} />

      {props.generated && (
        <div sx={styles.status} role="status">Expected output generated. Review it before adding the test case.</div>
      )}

      <ProblemCreationField
        label="Hidden test expected output (JSON)"
        value={draft.expectedOutputJson}
        readOnly
        hint="Generated from the reference solution using the input above."
        rows={4}
        maxLength={20_000}
        disabled
      />

      <ProblemCreationField
        label="Hidden test explanation (optional Markdown)"
        value={draft.explanationMarkdown}
        onChange={(explanationMarkdown) => onChange({...draft, explanationMarkdown})}
        rows={3}
        maxLength={10_000}
        disabled={isSaving}
      />

      {errors.length > 0 && !isSaving && (
        <div sx={styles.error} role="alert">
          {errors.map((message, index) => <div key={index}>{message}</div>)}
        </div>
      )}

      {saved && !isSaving && errors.length === 0 && (
        <div sx={styles.status} role="status">Hidden test case added.</div>
      )}

      <div sx={styles.actions}>
        <Control
          appearance={[
            styles.control,
            submitDisabled && styles.disabled
          ]}
          disabled={submitDisabled}
          onActivate={onSubmit}
        >
          {isSaving ? "Adding…" : "Add hidden test case"}
        </Control>
      </div>
    </div>
  );
}

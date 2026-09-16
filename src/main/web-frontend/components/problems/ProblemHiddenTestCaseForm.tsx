import * as stylex from "@stylexjs/stylex";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";

export type ProblemHiddenTestCaseDraft = {
  inputJson: string;
  expectedOutputJson: string;
  explanationMarkdown: string;
};

type Props = {
  draft: ProblemHiddenTestCaseDraft;
  onChange: (draft: ProblemHiddenTestCaseDraft) => void;
  onSubmit: () => void;
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
  columns: {
    display: "grid",
    gridTemplateColumns: {
      default: "1fr 1fr",
      "@media (max-width: 600px)": "1fr"
    },
    gap: 18
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
  const submitDisabled = isSaving ||
    !draft.inputJson.trim() ||
    !draft.expectedOutputJson.trim();

  return (
    <div sx={styles.form} aria-busy={isSaving}>
      <div sx={styles.description}>
        Hidden test cases are not shown to people solving the problem.
      </div>

      <div sx={styles.columns}>
        <ProblemCreationField
          label="Hidden test input (JSON)"
          value={draft.inputJson}
          onChange={(inputJson) => onChange({...draft, inputJson})}
          rows={4}
          maxLength={20_000}
          disabled={isSaving}
        />

        <ProblemCreationField
          label="Hidden test expected output (JSON)"
          value={draft.expectedOutputJson}
          onChange={(expectedOutputJson) => onChange({...draft, expectedOutputJson})}
          hint="The JSON value null is allowed."
          rows={4}
          maxLength={20_000}
          disabled={isSaving}
        />
      </div>

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

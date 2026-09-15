import type {ReactNode} from "react";
import * as stylex from "@stylexjs/stylex";
import Control from "components/coding/WorkspaceControl";
import ProblemCreationField from "./ProblemCreationField";
import type {CreateProblemExampleInput} from "__generated__/ProblemCreationFormMutation.graphql";

type Props = {
  example: CreateProblemExampleInput;
  index: number;
  disabled: boolean;
  canRemove: boolean;
  onChange: (example: CreateProblemExampleInput) => void;
  onRemove?: () => void;
  children?: ReactNode;
};

const styles = stylex.create({
  example: {
    padding: 20,
    border: "1px solid #393b40",
    borderRadius: 8,
    display: "flex",
    flexDirection: "column",
    gap: 18
  },

  heading: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center"
  },

  title: {
    fontWeight: 600
  },

  remove: {
    color: "#9da0a8",
    cursor: "pointer",
    padding: 6,
    borderRadius: 4
  },

  columns: {
    display: "grid",
    gridTemplateColumns: {
      default: "1fr 1fr",
      "@media (max-width: 600px)": "1fr"
    },
    gap: 18
  }
});

export default function ProblemExampleEditor({
  example,
  index,
  disabled,
  canRemove,
  onChange,
  onRemove,
  children,
}: Props) {
  return (
    <div sx={styles.example}>
      <div sx={styles.heading}>
        <span sx={styles.title}>Example {index + 1}</span>

        {canRemove && (
          <Control
            appearance={styles.remove}
            disabled={disabled}
            onActivate={onRemove}
            label={`Remove example ${index + 1}`}
          >
            Remove
          </Control>
        )}
      </div>

      <div sx={styles.columns}>
        <ProblemCreationField
          label={`Example ${index + 1} input (JSON)`}
          value={example.inputJson}
          onChange={(inputJson) => onChange({...example, inputJson})}
          rows={3}
          maxLength={20_000}
          disabled={disabled}
        />

        <ProblemCreationField
          label={`Example ${index + 1} expected output (JSON)`}
          value={example.expectedOutputJson}
          onChange={(expectedOutputJson) => onChange({...example, expectedOutputJson})}
          rows={3}
          maxLength={20_000}
          disabled={disabled}
        />
      </div>

      <ProblemCreationField
        label={`Example ${index + 1} explanation (optional Markdown)`}
        value={example.explanationMarkdown ?? ""}
        onChange={(explanationMarkdown) => onChange({...example, explanationMarkdown})}
        rows={2}
        maxLength={10_000}
        disabled={disabled}
      />

      {children}
    </div>
  );
}

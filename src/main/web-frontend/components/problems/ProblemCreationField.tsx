import {useId} from "react";
import * as stylex from "@stylexjs/stylex";

type Props = {
  label: string;
  value: string;
  hint?: string;
  error?: string;
  rows?: number;
  maxLength?: number;
  type?: "text" | "number";
  min?: number;
  max?: number;
  disabled: boolean;
} & (
  | {readOnly: true; onChange?: never}
  | {readOnly?: false; onChange: (value: string) => void}
);

const styles = stylex.create({
  field: {
    display: "flex",
    flexDirection: "column",
    gap: 8
  },

  label: {
    fontWeight: 600
  },

  input: {
    width: "100%",
    boxSizing: "border-box",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 5,
    padding: "10px 12px",
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: "inherit",
    fontSize: 14,
    lineHeight: 1.5,
    resize: "vertical",
    outlineColor: "#6b9bfa"
  },

  hint: {
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.5
  },
  error: {
    color: "#f2a6a6",
    fontSize: 12,
    lineHeight: 1.5
  }
});

export default function ProblemCreationField({
  label,
  value,
  onChange,
  readOnly = false,
  hint,
  error,
  rows,
  maxLength,
  type = "text",
  min,
  max,
  disabled,
}: Props) {
  const id = useId();

  const props = {
    value,
    onChange: (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange?.(event.target.value),
    readOnly,
    "aria-labelledby": id,
    "aria-describedby": [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(" ") || undefined,
    "aria-invalid": Boolean(error),
    maxLength,
    disabled,
  };

  return (
    <div sx={styles.field}>
      <span id={id} sx={styles.label}>{label}</span>

      {rows ? (
        <textarea {...props} sx={styles.input} rows={rows} />
      ) : (
        <input {...props} sx={styles.input} type={type} min={min} max={max} />
      )}

      {hint && <span id={`${id}-hint`} sx={styles.hint}>{hint}</span>}
      {error && <span id={`${id}-error`} sx={styles.error}>{error}</span>}
    </div>
  );
}

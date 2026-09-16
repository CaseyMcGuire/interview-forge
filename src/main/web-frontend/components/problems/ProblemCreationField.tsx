import {useId} from "react";
import * as stylex from "@stylexjs/stylex";

type Props = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  hint?: string;
  rows?: number;
  maxLength?: number;
  type?: "text" | "number";
  min?: number;
  max?: number;
  disabled: boolean;
};

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
    border: "1px solid #4e5157",
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
  }
});

export default function ProblemCreationField({
  label,
  value,
  onChange,
  hint,
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
    onChange: (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange(event.target.value),
    "aria-labelledby": id,
    "aria-describedby": hint ? `${id}-hint` : undefined,
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
    </div>
  );
}

import * as stylex from "@stylexjs/stylex";
import {useEffect, useId, useRef, useState, type FormEvent} from "react";
import {formatTestCaseJson} from "./formatTestCaseJson";

type Props = {
  testNumber: number;
  inputJson: string;
  disabled: boolean;
  onSave: (inputJson: string) => void;
  onClose: () => void;
};

const styles = stylex.create({
  dialog: {
    width: "min(800px, calc(100vw - 32px))",
    maxWidth: "none",
    maxHeight: "calc(100dvh - 32px)",
    margin: "auto",
    padding: 24,
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 10,
    backgroundColor: {
      default: "#1e1f22",
      "::backdrop": "rgb(0 0 0 / 60%)"
    },
    color: "#dfe1e5",
    boxShadow: "0 20px 60px rgb(0 0 0 / 40%)"
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 16
  },
  title: {
    fontSize: 16,
    fontWeight: 600
  },
  label: {
    fontSize: 13,
    color: "#9da0a8"
  },
  field: {
    width: "100%",
    height: "min(480px, 50dvh)",
    minHeight: 120,
    padding: 12,
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: {
      default: "#4e5157",
      ":focus": "#3574f0"
    },
    borderRadius: 5,
    backgroundColor: "#16171a",
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 13,
    lineHeight: 1.6,
    color: "#dfe1e5",
    tabSize: 2,
    resize: "vertical",
    outline: "none"
  },
  error: {
    fontSize: 13,
    color: "#f2a6a6"
  },
  actions: {
    display: "flex",
    justifyContent: "flex-end",
    gap: 8
  },
  button: {
    padding: "8px 14px",
    borderWidth: 1,
    borderStyle: "solid",
    borderColor: "#4e5157",
    borderRadius: 5,
    backgroundColor: "#2b2d30",
    color: "#dfe1e5",
    fontFamily: "inherit",
    fontSize: 13,
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  saveButton: {
    borderColor: "#3574f0",
    backgroundColor: "#3574f0",
    color: "#ffffff"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

/** Keeps edits local until Save; the native dialog handles modal focus and Escape. */
export default function TestInputDialog(props: Props) {
  const {testNumber, inputJson, disabled, onSave, onClose} = props;
  const dialogRef = useRef<HTMLDialogElement>(null);
  const id = useId();
  const [draft, setDraft] = useState(() => formatTestCaseJson(inputJson));
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (disabled) {
      return;
    }

    try {
      JSON.parse(draft);
    } catch {
      setError("Enter valid JSON before saving.");
      return;
    }

    dialogRef.current?.close();

    // Opening and saving without edits should preserve the input's existing test result.
    if (draft !== formatTestCaseJson(inputJson)) {
      onSave(formatTestCaseJson(draft));
    }

    onClose();
  }

  return (
    <dialog
      ref={dialogRef}
      sx={styles.dialog}
      aria-labelledby={`${id}-title`}
      onCancel={event => {
        event.preventDefault();
        close();
      }}
    >
      <form sx={styles.form} onSubmit={save}>
        <h2 id={`${id}-title`} sx={styles.title}>Edit test {testNumber} input</h2>
        <label htmlFor={`${id}-input`} sx={styles.label}>JSON input</label>
        <textarea
          id={`${id}-input`}
          sx={styles.field}
          value={draft}
          onChange={event => {
            setDraft(event.target.value);
            setError(null);
          }}
          disabled={disabled}
          autoFocus
          spellCheck={false}
          autoCapitalize="off"
          autoCorrect="off"
          wrap="off"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
        />
        {error && <p id={`${id}-error`} sx={styles.error} role="alert">{error}</p>}
        <div sx={styles.actions}>
          <button type="button" sx={styles.button} onClick={close}>Cancel</button>
          <button
            type="submit"
            sx={[styles.button, styles.saveButton, disabled && styles.disabled]}
            disabled={disabled}
          >
            Save input
          </button>
        </div>
      </form>
    </dialog>
  );
}

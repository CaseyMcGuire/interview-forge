import * as stylex from "@stylexjs/stylex";
import {useId} from "react";
import Control from "./WorkspaceControl";

export type CustomInputDraft = {
  id: string;
  inputJson: string;
};

type Props = {
  cases: readonly CustomInputDraft[];
  maxCases: number;
  onChange: (cases: CustomInputDraft[]) => void;
  disabled?: boolean;
  errors?: Readonly<Record<string, string>>;
};

const styles = stylex.create({
  panel: {
    minWidth: 0,
    minHeight: 0,
    display: "flex",
    flexDirection: "column",
    color: "#bcbec4"
  },
  heading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    flexWrap: "wrap",
    gap: 12,
    padding: "12px 22px",
    borderBottom: "1px solid #393b40"
  },
  title: {
    margin: 0,
    fontSize: 12,
    fontWeight: 600
  },
  count: {
    color: "#9da0a8",
    fontSize: 11
  },
  body: {
    padding: "16px 22px",
    overflowY: "auto",
    scrollbarWidth: "thin",
    scrollbarColor: "#4e5157 transparent"
  },
  description: {
    margin: "0 0 16px",
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.6
  },
  cases: {
    display: "flex",
    flexDirection: "column",
    gap: 16
  },
  caseHeading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    marginBottom: 6,
    fontSize: 12
  },
  input: {
    display: "block",
    boxSizing: "border-box",
    width: "100%",
    minHeight: 88,
    padding: "9px 12px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    backgroundColor: "#1e1f22",
    color: "#dfe1e5",
    fontFamily: '"SFMono-Regular", Consolas, monospace',
    fontSize: 12,
    lineHeight: 1.7,
    resize: "vertical",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  invalid: {
    borderColor: "#f2a6a6"
  },
  control: {
    width: "fit-content",
    padding: "5px 10px",
    border: "1px solid #4e5157",
    borderRadius: 5,
    color: "#dfe1e5",
    fontSize: 12,
    cursor: "pointer",
    outline: {
      default: "none",
      ":focus-visible": "2px solid #3574f0"
    },
    outlineOffset: 2
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  },
  footer: {
    marginTop: 16
  },
  error: {
    marginTop: 6,
    fontSize: 12,
    color: "#f2a6a6",
    overflowWrap: "anywhere"
  }
});

/** Keeps raw JSON drafts in the parent so editing and running can have separate lifetimes. */
export default function CustomInputEditor(props: Props) {
  const {cases, maxCases, onChange, disabled = false, errors = {}} = props;
  const id = useId();
  const addDisabled = disabled || cases.length >= maxCases;
  const removeDisabled = disabled || cases.length <= 1;

  function addCase() {
    if (addDisabled) {
      return;
    }

    onChange([...cases, {id: crypto.randomUUID(), inputJson: ""}]);
  }

  function updateCase(caseId: string, inputJson: string) {
    onChange(cases.map(testCase => testCase.id === caseId ? {...testCase, inputJson} : testCase));
  }

  function removeCase(caseId: string) {
    if (removeDisabled) {
      return;
    }

    onChange(cases.filter(testCase => testCase.id !== caseId));
  }

  return (
    <section sx={styles.panel} aria-labelledby={`${id}-heading`}>
      <div sx={styles.heading}>
        <h2 id={`${id}-heading`} sx={styles.title}>Test inputs</h2>
        <span role="status" sx={styles.count}>{cases.length} / {maxCases} tests</span>
      </div>

      <div sx={styles.body}>
        <p id={`${id}-description`} sx={styles.description}>
          Enter one JSON value per test. Expected outputs are calculated automatically.
        </p>

        <div sx={styles.cases}>
          {cases.map((testCase, index) => {
            const inputId = `${id}-${testCase.id}`;
            const error = errors[testCase.id];

            return (
              <div key={testCase.id}>
                <div sx={styles.caseHeading}>
                  <label htmlFor={inputId}>Test {index + 1} input</label>
                  <Control
                    label={`Remove test ${index + 1}`}
                    appearance={[styles.control, removeDisabled && styles.disabled]}
                    disabled={removeDisabled}
                    onActivate={() => removeCase(testCase.id)}
                  >
                    Remove
                  </Control>
                </div>

                <textarea
                  id={inputId}
                  sx={[styles.input, Boolean(error) && styles.invalid, disabled && styles.disabled]}
                  value={testCase.inputJson}
                  onChange={event => updateCase(testCase.id, event.target.value)}
                  disabled={disabled}
                  rows={4}
                  spellCheck={false}
                  autoCapitalize="off"
                  autoCorrect="off"
                  aria-invalid={Boolean(error)}
                  aria-describedby={error ? `${inputId}-error` : `${id}-description`}
                />
                {error && <div id={`${inputId}-error`} sx={styles.error} role="alert">{error}</div>}
              </div>
            );
          })}
        </div>

        <div sx={styles.footer}>
          <Control
            appearance={[styles.control, addDisabled && styles.disabled]}
            disabled={addDisabled}
            onActivate={addCase}
          >
            Add test
          </Control>
        </div>
      </div>
    </section>
  );
}

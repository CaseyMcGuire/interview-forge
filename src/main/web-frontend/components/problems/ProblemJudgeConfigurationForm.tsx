import {useId} from "react";
import * as stylex from "@stylexjs/stylex";
import CodeEditor from "components/coding/CodeEditor";
import Control from "components/coding/WorkspaceControl";
import type {
  ProblemTestDriverField_language$key,
  TestDriverType
} from "__generated__/ProblemTestDriverField_language.graphql";
import ProblemCreationField from "./ProblemCreationField";
import ProblemEditFeedback from "./ProblemEditFeedback";
import ProblemTestDriverField from "./ProblemTestDriverField";

export type JudgeLimit = {
  label: string;
  unit: string;
  min: number;
  max: number;
};

/** Bounds shared by the inputs, their hints, and the save-time parse; the server enforces the same ranges. */
export const judgeLimits = {
  timeLimitMs: {label: "Time limit", unit: "milliseconds", min: 1, max: 60_000},
  memoryLimitMb: {label: "Memory limit", unit: "megabytes", min: 1, max: 8_192}
} satisfies Record<string, JudgeLimit>;

function limitHint(limit: JudgeLimit): string {
  return `Per test case, from ${limit.min} to ${limit.max.toLocaleString("en-US")} ${limit.unit}.`;
}

/** Limits stay as text while editing so the fields can be emptied; they are parsed on save. */
export type ProblemJudgeConfigurationDraft = {
  testDriverType: TestDriverType | null;
  testDriverCode: string;
  timeLimitMs: string;
  memoryLimitMb: string;
};

type Props = {
  language: ProblemTestDriverField_language$key;
  languageKey: string;
  languageName: string;
  draft: ProblemJudgeConfigurationDraft;
  exists: boolean;
  onChange: (draft: ProblemJudgeConfigurationDraft) => void;
  onSubmit: () => void;
  submitDisabled: boolean;
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
  editor: {
    height: 280,
    overflow: "hidden",
    border: "1px solid #4e5157",
    borderRadius: 5
  },
  label: {
    fontWeight: 600
  },
  help: {
    color: "#9da0a8",
    fontSize: 12,
    lineHeight: 1.5
  },
  limits: {
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
  save: {
    padding: "10px 16px",
    borderRadius: 5,
    width: "fit-content",
    backgroundColor: "#3574f0",
    color: "#ffffff",
    cursor: "pointer"
  },
  disabled: {
    opacity: 0.6,
    cursor: "default"
  }
});

export default function ProblemJudgeConfigurationForm(props: Props) {
  const {language, languageKey, languageName, draft, exists, onChange, onSubmit, submitDisabled, isSaving, errors, saved} = props;
  const id = useId();

  return (
    <div sx={styles.form} aria-busy={isSaving}>
      <ProblemTestDriverField
        language={language}
        value={draft.testDriverType}
        disabled={isSaving}
        onChange={(testDriverType) => onChange({...draft, testDriverType})}
      />

      <span sx={styles.label}>Test driver code</span>

      <div sx={styles.editor} inert={isSaving}>
        <CodeEditor
          languageKey={languageKey}
          label={`${languageName} test driver code`}
          describedBy={`${id}-driver-help`}
          value={draft.testDriverCode}
          onChange={(testDriverCode) => onChange({...draft, testDriverCode})}
          fontSize={14}
          wordWrap
          onCursorChange={() => {}}
        />
      </div>

      <div id={`${id}-driver-help`} sx={styles.help}>
        Compiled alongside the submitted solution using the selected driver type.
        Tab indents code. Press Escape, then Tab to leave the editor.
      </div>

      <div sx={styles.limits}>
        <ProblemCreationField
          label="Time limit (ms)"
          value={draft.timeLimitMs}
          onChange={(timeLimitMs) => onChange({...draft, timeLimitMs})}
          hint={limitHint(judgeLimits.timeLimitMs)}
          type="number"
          min={judgeLimits.timeLimitMs.min}
          max={judgeLimits.timeLimitMs.max}
          disabled={isSaving}
        />

        <ProblemCreationField
          label="Memory limit (MB)"
          value={draft.memoryLimitMb}
          onChange={(memoryLimitMb) => onChange({...draft, memoryLimitMb})}
          hint={limitHint(judgeLimits.memoryLimitMb)}
          type="number"
          min={judgeLimits.memoryLimitMb.min}
          max={judgeLimits.memoryLimitMb.max}
          disabled={isSaving}
        />
      </div>

      <ProblemEditFeedback errors={errors} saved={saved} />

      <div sx={styles.actions}>
        <Control
          appearance={[styles.save, submitDisabled && styles.disabled]}
          disabled={submitDisabled}
          onActivate={onSubmit}
        >
          {isSaving ? "Saving…" : exists ? "Save judge configuration" : "Create judge configuration"}
        </Control>
      </div>
    </div>
  );
}

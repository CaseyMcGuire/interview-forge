import {useState} from "react";
import {graphql, useFragment} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemJudgeConfigurationEditor_configuration$key} from "__generated__/ProblemJudgeConfigurationEditor_configuration.graphql";
import ProblemJudgeConfigurationForm, {type ProblemJudgeConfigurationDraft} from "./ProblemJudgeConfigurationForm";

type Props = {
  configuration: ProblemJudgeConfigurationEditor_configuration$key;
};

type Judge = {
  readonly runtime: string;
  readonly testDriverCode: string;
  readonly checkerSource: string | null | undefined;
  readonly timeLimitMs: number;
  readonly memoryLimitMb: number;
};

const styles = stylex.create({
  section: {
    display: "flex",
    flexDirection: "column",
    gap: 18
  },
  heading: {
    fontWeight: 600
  },
  description: {
    color: "#9da0a8",
    lineHeight: 1.6
  }
});

const emptyDraft: ProblemJudgeConfigurationDraft = {
  runtime: "",
  testDriverCode: "",
  checkerSource: "",
  timeLimitMs: "",
  memoryLimitMb: ""
};

function draftFrom(judge: Judge | null | undefined): ProblemJudgeConfigurationDraft {
  if (!judge) {
    return emptyDraft;
  }

  return {
    runtime: judge.runtime,
    testDriverCode: judge.testDriverCode,
    checkerSource: judge.checkerSource ?? "",
    timeLimitMs: String(judge.timeLimitMs),
    memoryLimitMb: String(judge.memoryLimitMb)
  };
}

function isComplete(draft: ProblemJudgeConfigurationDraft): boolean {
  return draft.runtime.trim() !== "" &&
    draft.testDriverCode.trim() !== "" &&
    draft.timeLimitMs.trim() !== "" &&
    draft.memoryLimitMb.trim() !== "";
}

function isSameDraft(a: ProblemJudgeConfigurationDraft, b: ProblemJudgeConfigurationDraft): boolean {
  return a.runtime === b.runtime &&
    a.testDriverCode === b.testDriverCode &&
    a.checkerSource === b.checkerSource &&
    a.timeLimitMs === b.timeLimitMs &&
    a.memoryLimitMb === b.memoryLimitMb;
}

export default function ProblemJudgeConfigurationEditor({configuration}: Props) {
  const data = useFragment(graphql`
    fragment ProblemJudgeConfigurationEditor_configuration on ProblemLanguage {
      id
      language {
        key
        displayName
      }
      judgeConfiguration {
        id
        runtime
        testDriverCode
        checkerSource
        timeLimitMs
        memoryLimitMb
      }
    }
  `, configuration);

  const saved = draftFrom(data.judgeConfiguration);
  const [draft, setDraft] = useState<ProblemJudgeConfigurationDraft>(saved);
  const exists = data.judgeConfiguration != null;
  const hasChanges = !isSameDraft(draft, saved);

  return (
    <div sx={styles.section}>
      <div sx={styles.heading} role="heading" aria-level={4}>
        Judge configuration
      </div>

      <div sx={styles.description}>
        {exists
          ? "Private settings used to run and grade submissions in this language. Not shown to solvers."
          : "No judge configuration yet. Submissions in this language cannot be run until one is created."}
      </div>

      <ProblemJudgeConfigurationForm
        languageKey={data.language.key}
        languageName={data.language.displayName}
        draft={draft}
        exists={exists}
        onChange={setDraft}
        onSubmit={() => {}}
        submitDisabled={!hasChanges || !isComplete(draft)}
        isSaving={false}
        errors={[]}
        saved={false}
      />
    </div>
  );
}

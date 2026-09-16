import {useState} from "react";
import {graphql, useFragment, useMutation} from "react-relay";
import * as stylex from "@stylexjs/stylex";
import type {ProblemJudgeConfigurationEditor_configuration$key} from "__generated__/ProblemJudgeConfigurationEditor_configuration.graphql";
import type {
  ProblemJudgeConfigurationEditorCreateMutation,
  ProblemJudgeConfigurationEditorCreateMutation$data
} from "__generated__/ProblemJudgeConfigurationEditorCreateMutation.graphql";
import type {
  ProblemJudgeConfigurationEditorUpdateMutation,
  ProblemJudgeConfigurationEditorUpdateMutation$data,
  UpdateJudgeConfigurationInput
} from "__generated__/ProblemJudgeConfigurationEditorUpdateMutation.graphql";
import ProblemJudgeConfigurationForm, {
  judgeLimits,
  type JudgeLimit,
  type ProblemJudgeConfigurationDraft
} from "./ProblemJudgeConfigurationForm";

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

type Result =
  | ProblemJudgeConfigurationEditorCreateMutation$data["createJudgeConfiguration"]
  | ProblemJudgeConfigurationEditorUpdateMutation$data["updateJudgeConfiguration"];

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

const failedMessage = "The judge configuration could not be saved. Your changes are still here; please try again.";

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

/** Returns the parsed limit, or the message to show when it is not a whole number within bounds. */
function parseLimit(value: string, limit: JudgeLimit): number | string {
  const parsed = Number(value);

  return Number.isInteger(parsed) && parsed >= limit.min && parsed <= limit.max
    ? parsed
    : `${limit.label} must be a whole number from ${limit.min} to ${limit.max.toLocaleString("en-US")} ${limit.unit}.`;
}

/** Maps the shared failure results to messages; the server's messages already name the field. */
function failureMessages(result: Result): readonly string[] {
  switch (result.__typename) {
    case "ProblemValidationFailure":
      return result.fieldErrors.length > 0
        ? result.fieldErrors.map((error) => error.message)
        : [result.message];

    case "ProblemNotFound":
    case "ProblemForbidden":
      return [result.message];

    default:
      return [failedMessage];
  }
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

  const [create, isCreating] = useMutation<ProblemJudgeConfigurationEditorCreateMutation>(graphql`
    mutation ProblemJudgeConfigurationEditorCreateMutation($input: CreateJudgeConfigurationInput!) {
      createJudgeConfiguration(input: $input) {
        __typename
        ... on CreateJudgeConfigurationSuccess {
          problemLanguage {
            id
            judgeConfiguration {
              id
              runtime
              testDriverCode
              checkerSource
              timeLimitMs
              memoryLimitMb
            }
          }
        }
        ... on ProblemValidationFailure {
          message
          fieldErrors {
            message
          }
        }
        ... on ProblemNotFound {
          message
        }
        ... on ProblemForbidden {
          message
        }
      }
    }
  `);

  const [update, isUpdating] = useMutation<ProblemJudgeConfigurationEditorUpdateMutation>(graphql`
    mutation ProblemJudgeConfigurationEditorUpdateMutation($input: UpdateJudgeConfigurationInput!) {
      updateJudgeConfiguration(input: $input) {
        __typename
        ... on UpdateJudgeConfigurationSuccess {
          judgeConfiguration {
            id
            runtime
            testDriverCode
            checkerSource
            timeLimitMs
            memoryLimitMb
          }
        }
        ... on ProblemValidationFailure {
          message
          fieldErrors {
            message
          }
        }
        ... on ProblemNotFound {
          message
        }
        ... on ProblemForbidden {
          message
        }
      }
    }
  `);

  const stored = draftFrom(data.judgeConfiguration);
  const [draft, setDraft] = useState<ProblemJudgeConfigurationDraft>(stored);
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [saved, setSaved] = useState(false);
  const isSaving = isCreating || isUpdating;
  const hasChanges = !isSameDraft(draft, stored);
  const submitDisabled = isSaving || !hasChanges || !isComplete(draft);

  function changeDraft(updated: ProblemJudgeConfigurationDraft) {
    setDraft(updated);
    setErrors([]);
    setSaved(false);
  }

  function finish(judge: Judge | null | undefined) {
    setDraft(draftFrom(judge));
    setSaved(true);
  }

  function save() {
    if (submitDisabled) {
      return;
    }

    const timeLimitMs = parseLimit(draft.timeLimitMs, judgeLimits.timeLimitMs);
    const memoryLimitMb = parseLimit(draft.memoryLimitMb, judgeLimits.memoryLimitMb);

    if (typeof timeLimitMs === "string" || typeof memoryLimitMb === "string") {
      setErrors([timeLimitMs, memoryLimitMb].filter((limit): limit is string => typeof limit === "string"));
      return;
    }

    setErrors([]);
    setSaved(false);
    // An empty checker means none: omitted on creation, cleared on update.
    const checkerSource = draft.checkerSource.trim() === "" ? null : draft.checkerSource;
    const current = data.judgeConfiguration;

    if (!current) {
      create({
        variables: {
          input: {
            problemLanguageId: data.id,
            runtime: draft.runtime,
            testDriverCode: draft.testDriverCode,
            checkerSource,
            timeLimitMs,
            memoryLimitMb
          }
        },

        onCompleted: (response, graphqlErrors) => {
          const result = response.createJudgeConfiguration;

          if (graphqlErrors?.length || !result) {
            setErrors([failedMessage]);
          } else if (result.__typename === "CreateJudgeConfigurationSuccess") {
            finish(result.problemLanguage.judgeConfiguration);
          } else {
            setErrors(failureMessages(result));
          }
        },

        onError: () => {
          setErrors([failedMessage]);
        }
      });
      return;
    }

    const input: UpdateJudgeConfigurationInput = {id: current.id};

    if (draft.runtime !== current.runtime) {
      input.runtime = draft.runtime;
    }

    if (draft.testDriverCode !== current.testDriverCode) {
      input.testDriverCode = draft.testDriverCode;
    }

    if (checkerSource !== (current.checkerSource ?? null)) {
      input.checkerSource = checkerSource;
    }

    if (timeLimitMs !== current.timeLimitMs) {
      input.timeLimitMs = timeLimitMs;
    }

    if (memoryLimitMb !== current.memoryLimitMb) {
      input.memoryLimitMb = memoryLimitMb;
    }

    update({
      variables: {input},

      onCompleted: (response, graphqlErrors) => {
        const result = response.updateJudgeConfiguration;

        if (graphqlErrors?.length || !result) {
          setErrors([failedMessage]);
        } else if (result.__typename === "UpdateJudgeConfigurationSuccess") {
          finish(result.judgeConfiguration);
        } else {
          setErrors(failureMessages(result));
        }
      },

      onError: () => {
        setErrors([failedMessage]);
      }
    });
  }

  return (
    <div sx={styles.section}>
      <div sx={styles.heading} role="heading" aria-level={4}>
        Judge configuration
      </div>

      <div sx={styles.description}>
        {data.judgeConfiguration
          ? "Private settings used to run and grade submissions in this language. Not shown to solvers."
          : "No judge configuration yet. Submissions in this language cannot be run until one is created."}
      </div>

      <ProblemJudgeConfigurationForm
        languageKey={data.language.key}
        languageName={data.language.displayName}
        draft={draft}
        exists={data.judgeConfiguration != null}
        onChange={changeDraft}
        onSubmit={save}
        submitDisabled={submitDisabled}
        isSaving={isSaving}
        errors={errors}
        saved={saved && !hasChanges}
      />
    </div>
  );
}

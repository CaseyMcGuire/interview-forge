import {useEffect, useRef, useState} from "react";
import {graphql, useMutation} from "react-relay";
import type {Disposable} from "relay-runtime";
import type {useEnqueueCustomInputSubmissionMutation} from "__generated__/useEnqueueCustomInputSubmissionMutation.graphql";

type Feedback = {
  message: string;
  fieldErrors?: Readonly<Record<string, string>>;
  requiresSignIn?: boolean;
};

/**
 * Queues the editor source and raw test inputs, tracks the request, and exposes
 * validation or admission failures. Passes the accepted ID to the workspace for
 * navigation and polling. Never retries automatically: the request may already
 * have created a submission even when its response is lost.
 */
export default function useEnqueueCustomInputSubmission(onAccepted: (submissionId: string) => void) {
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const activeRequest = useRef<Disposable | null>(null);
  const [commit, isEnqueuing] = useMutation<useEnqueueCustomInputSubmissionMutation>(graphql`
    mutation useEnqueueCustomInputSubmissionMutation($input: EnqueueCustomInputSubmissionInput!) {
      enqueueCustomInputSubmission(input: $input) {
        __typename
        ... on EnqueueCustomInputSubmissionSuccess {
          customInputSubmissionId
        }
        ... on EnqueueCustomInputSubmissionValidationFailure {
          message
          fieldErrors {
            field
            message
          }
        }
        ... on AuthenticationRequired {
          message
        }
        ... on ProblemNotFound {
          message
        }
        ... on ExecutionUnavailable {
          message
        }
        ... on ExecutionBusy {
          message
        }
      }
    }
  `);

  // Leaving this problem must also prevent a late response from changing its URL.
  useEffect(() => () => activeRequest.current?.dispose(), []);

  function enqueueCustomInputSubmission(problemLanguageId: string, sourceCode: string, caseInputs: readonly string[]) {
    if (activeRequest.current) {
      return;
    }

    setFeedback(null);

    activeRequest.current = commit({
      variables: {
        input: {
          problemLanguageId,
          sourceCode,
          cases: caseInputs.map(inputJson => ({inputJson}))
        }
      },

      onCompleted: (response, errors) => {
        activeRequest.current = null;
        const result = response.enqueueCustomInputSubmission;

        if (errors?.length || !result) {
          setFeedback({message: "We couldn't confirm your test request. Your code and inputs are still here."});
          return;
        }

        switch (result.__typename) {
          case "EnqueueCustomInputSubmissionSuccess":
            onAccepted(result.customInputSubmissionId);
            return;

          case "EnqueueCustomInputSubmissionValidationFailure":
            setFeedback({
              message: result.fieldErrors.length > 0
                ? result.fieldErrors.map(error => error.message).join("\n")
                : result.message,
              fieldErrors: Object.fromEntries(result.fieldErrors.map(error => [error.field, error.message]))
            });
            return;

          case "AuthenticationRequired":
            setFeedback({message: result.message, requiresSignIn: true});
            return;

          case "ProblemNotFound":
          case "ExecutionUnavailable":
          case "ExecutionBusy":
            setFeedback({message: result.message});
            return;

          default:
            setFeedback({message: "We couldn't confirm your test request. Your code and inputs are still here."});
        }
      },

      onError: () => {
        activeRequest.current = null;
        setFeedback({message: "We couldn't confirm your test request. Check your connection before trying again. Your code and inputs are still here."});
      }
    });
  }

  function clearFeedback() {
    setFeedback(null);
  }

  return {
    enqueueCustomInputSubmission,
    isEnqueuing,
    error: feedback?.message ?? null,
    fieldErrors: feedback?.fieldErrors ?? {},
    requiresSignIn: feedback?.requiresSignIn ?? false,
    clearFeedback
  };
}

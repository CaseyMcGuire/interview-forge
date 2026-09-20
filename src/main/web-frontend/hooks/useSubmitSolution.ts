import {useEffect, useRef, useState} from "react";
import {graphql, useMutation} from "react-relay";
import type {Disposable} from "relay-runtime";
import type {useSubmitSolutionMutation} from "__generated__/useSubmitSolutionMutation.graphql";

type Feedback = {
  message: string;
  requiresSignIn?: boolean;
};

/**
 * Submits the language configuration ID and editor source, tracks request progress,
 * and turns API failures into UI feedback. Calls onProblemSubmissionAccepted with the new
 * submission ID so the workspace can update the URL and begin status polling.
 * Requests are not retried automatically because the server may already have accepted them.
 */
export default function useSubmitSolution(onProblemSubmissionAccepted: (problemSubmissionId: string) => void) {
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const activeRequest = useRef<Disposable | null>(null);
  const [commit, isSubmitting] = useMutation<useSubmitSolutionMutation>(graphql`
    mutation useSubmitSolutionMutation($input: SubmitSolutionInput!) {
      submitSolution(input: $input) {
        __typename
        ... on SubmitSolutionSuccess {
          problemSubmission {
            id
          }
        }
        ... on ProblemSubmissionValidationFailure {
          message
          fieldErrors {
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

  // A response after leaving the problem must not navigate back to its submission.
  useEffect(() => () => activeRequest.current?.dispose(), []);

  function submitSolution(problemLanguageId: string, sourceCode: string) {
    if (activeRequest.current) {
      return;
    }

    setFeedback(null);

    activeRequest.current = commit({
      variables: {input: {problemLanguageId, sourceCode}},

      onCompleted: (response, errors) => {
        activeRequest.current = null;
        const result = response.submitSolution;

        if (errors?.length || !result) {
          setFeedback({message: "We couldn't confirm your submission. Your code is still here."});
          return;
        }

        switch (result.__typename) {
          case "SubmitSolutionSuccess":
            onProblemSubmissionAccepted(result.problemSubmission.id);
            return;

          case "ProblemSubmissionValidationFailure":
            setFeedback({
              message: result.fieldErrors.length > 0
                ? result.fieldErrors.map(error => error.message).join("\n")
                : result.message
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
            setFeedback({message: "We couldn't confirm your submission. Your code is still here."});
        }
      },

      onError: () => {
        activeRequest.current = null;
        setFeedback({message: "We couldn't confirm your submission. Check your connection before trying again. Your code is still here."});
      }
    });
  }

  return {
    submitSolution,
    isSubmitting,
    error: feedback?.message ?? null,
    requiresSignIn: feedback?.requiresSignIn ?? false
  };
}

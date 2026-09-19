import {useEffect, useState} from "react";
import {fetchQuery, graphql, useRelayEnvironment} from "react-relay";
import {createOperationDescriptor, getRequest, type Subscription} from "relay-runtime";
import type {useSubmissionStatusQuery} from "__generated__/useSubmissionStatusQuery.graphql";

type StatusState = {
  submissionId: string;
  submission: useSubmissionStatusQuery["response"]["submission"];
  requestStatus: "loading" | "loaded" | "failed";
  error: string | null;
};

const submissionStatusQuery = graphql`
  query useSubmissionStatusQuery($id: ID!) @throwOnFieldError {
    submission(id: $id) {
      id
      status
      ...SubmissionResultPanel_submission
    }
  }
`;

/**
 * Loads a persisted submission by ID and polls about once per second while it is
 * queued or running. Stops when execution finishes, the submission is unavailable,
 * a request fails, or the workspace closes. Exposes the latest result, loading and
 * error state, and a manual retry function for failed status requests.
 */
export default function useSubmissionStatus(submissionId: string | null) {
  const environment = useRelayEnvironment();
  const [state, setState] = useState<StatusState | null>(null);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    if (!submissionId) {
      setState(null);
      return;
    }

    setState(previous => ({
      submissionId,
      submission: previous?.submissionId === submissionId ? previous.submission : null,
      requestStatus: "loading",
      error: null
    }));

    // Keep fragment data available after polling stops and until this result is no longer shown.
    const operation = createOperationDescriptor(getRequest(submissionStatusQuery), {id: submissionId});
    const retainedResult = environment.retain(operation);
    let polling: Subscription;

    fetchQuery<useSubmissionStatusQuery>(environment, submissionStatusQuery, {id: submissionId}, {
      fetchPolicy: "network-only"
    }).poll(1_000).subscribe({
      start: subscription => {
        polling = subscription;
      },

      next: response => {
        const submission = response.submission;

        setState({
          submissionId,
          submission,
          requestStatus: "loaded",
          error: submission ? null : "This submission is unavailable. It may not exist or belong to your signed-in account."
        });

        if (submission?.status !== "QUEUED" && submission?.status !== "RUNNING") {
          polling.unsubscribe();
        }
      },

      error: () => {
        setState(previous => ({
          submissionId,
          submission: previous?.submissionId === submissionId ? previous.submission : null,
          requestStatus: "failed",
          error: "Couldn't refresh your submission. Retry the status check to see its result."
        }));
      }
    });

    return () => {
      polling.unsubscribe();
      retainedResult.dispose();
    };
  }, [environment, submissionId, retryCount]);

  const current = state?.submissionId === submissionId ? state : null;
  const submission = current?.submission ?? null;
  const isLoading = Boolean(submissionId) && (!current || current.requestStatus === "loading");
  const isPending = submission?.status === "QUEUED" || submission?.status === "RUNNING";
  const isUnresolved = isLoading || (current?.requestStatus === "failed" && !submission);

  function retrySubmissionStatus() {
    if (isLoading) {
      return;
    }

    setRetryCount(count => count + 1);
  }

  return {
    submission,
    isLoading,
    isPending,
    isUnresolved,
    error: current?.error ?? null,
    retrySubmissionStatus
  };
}

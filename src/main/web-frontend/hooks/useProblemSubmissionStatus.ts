import {useEffect, useState} from "react";
import {fetchQuery, graphql, useRelayEnvironment} from "react-relay";
import {createOperationDescriptor, getRequest, type Subscription} from "relay-runtime";
import type {useProblemSubmissionStatusQuery} from "__generated__/useProblemSubmissionStatusQuery.graphql";

type StatusState = {
  problemSubmissionId: string;
  problemSubmission: useProblemSubmissionStatusQuery["response"]["problemSubmission"];
  requestStatus: "loading" | "loaded" | "failed";
  error: string | null;
};

const problemSubmissionStatusQuery = graphql`
  query useProblemSubmissionStatusQuery($id: ID!) @throwOnFieldError {
    problemSubmission(id: $id) {
      id
      status
      ...ProblemSubmissionResultPanel_problemSubmission
    }
  }
`;

/**
 * Loads a persisted submission by ID and polls about once per second while it is
 * queued or running. Stops when execution finishes, the submission is unavailable,
 * a request fails, or the workspace closes. Exposes the latest result, loading and
 * error state, and a manual retry function for failed status requests.
 */
export default function useProblemSubmissionStatus(problemSubmissionId: string | null) {
  const environment = useRelayEnvironment();
  const [state, setState] = useState<StatusState | null>(null);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    if (!problemSubmissionId) {
      setState(null);
      return;
    }

    setState(previous => ({
      problemSubmissionId,
      problemSubmission: previous?.problemSubmissionId === problemSubmissionId ? previous.problemSubmission : null,
      requestStatus: "loading",
      error: null
    }));

    // Keep fragment data available after polling stops and until this result is no longer shown.
    const operation = createOperationDescriptor(getRequest(problemSubmissionStatusQuery), {id: problemSubmissionId});
    const retainedResult = environment.retain(operation);
    let polling: Subscription;

    fetchQuery<useProblemSubmissionStatusQuery>(environment, problemSubmissionStatusQuery, {id: problemSubmissionId}, {
      fetchPolicy: "network-only"
    }).poll(1_000).subscribe({
      start: subscription => {
        polling = subscription;
      },

      next: response => {
        const problemSubmission = response.problemSubmission;

        setState({
          problemSubmissionId,
          problemSubmission,
          requestStatus: "loaded",
          error: problemSubmission ? null : "This submission is unavailable. It may not exist or belong to your signed-in account."
        });

        if (problemSubmission?.status !== "QUEUED" && problemSubmission?.status !== "RUNNING") {
          polling.unsubscribe();
        }
      },

      error: () => {
        setState(previous => ({
          problemSubmissionId,
          problemSubmission: previous?.problemSubmissionId === problemSubmissionId ? previous.problemSubmission : null,
          requestStatus: "failed",
          error: "Couldn't refresh your submission. Retry the status check to see its result."
        }));
      }
    });

    return () => {
      polling.unsubscribe();
      retainedResult.dispose();
    };
  }, [environment, problemSubmissionId, retryCount]);

  const current = state?.problemSubmissionId === problemSubmissionId ? state : null;
  const problemSubmission = current?.problemSubmission ?? null;
  const isLoading = Boolean(problemSubmissionId) && (!current || current.requestStatus === "loading");
  const isPending = problemSubmission?.status === "QUEUED" || problemSubmission?.status === "RUNNING";
  const isUnresolved = isLoading || (current?.requestStatus === "failed" && !problemSubmission);

  function retryProblemSubmissionStatus() {
    if (isLoading) {
      return;
    }

    setRetryCount(count => count + 1);
  }

  return {
    problemSubmission,
    isLoading,
    isPending,
    isUnresolved,
    error: current?.error ?? null,
    retryProblemSubmissionStatus
  };
}

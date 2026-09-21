import {useRequest} from "ahooks";
import {useEffect, useRef} from "react";
import {fetchQuery, useRelayEnvironment} from "react-relay";
import {createOperationDescriptor, getRequest, type GraphQLTaggedNode, type Subscription} from "relay-runtime";

type Submission = {
  readonly status: string;
};

type PollingOptions<TResponse, TSubmission extends Submission> = {
  query: GraphQLTaggedNode;
  selectSubmission: (response: TResponse) => TSubmission | null | undefined;
  unavailableMessage: string;
  requestFailedMessage: string;
};

type StatusQuery<TResponse> = {
  variables: {id: string};
  response: TResponse;
};

/**
 * Polls a submission by ID until it finishes, becomes unavailable, or a request
 * fails. Retains its Relay fragments while displayed, preserves the last result
 * on errors, and exposes a manual retry. Cancels requests when the ID changes or
 * the caller unmounts. Keep options at module scope so renders don't restart polling.
 */
export default function useSubmissionPolling<TResponse, TSubmission extends Submission>(
  submissionId: string | null,
  options: PollingOptions<TResponse, TSubmission>
) {
  const environment = useRelayEnvironment();
  const request = useRef<Subscription | null>(null);
  const {data, loading, error, params, run, cancel, refresh, mutate} = useRequest(loadSubmission, {
    manual: true,
    pollingInterval: 1_000,
    pollingErrorRetryCount: 0,

    onSuccess(result) {
      if (!isSubmissionPending(result.submission)) {
        cancel();
      }
    }
  });

  useEffect(() => {
    mutate(undefined);

    if (!submissionId) {
      return;
    }

    // Keep fragment data available after polling stops and until the result is no longer shown.
    const operation = createOperationDescriptor(getRequest(options.query), {id: submissionId});
    const retainedResult = environment.retain(operation);
    run(submissionId);

    return () => {
      // ahooks ignores cancelled promises; also unsubscribe from the active Relay request.
      cancel();
      request.current?.unsubscribe();
      retainedResult.dispose();
    };
  }, [environment, submissionId, options, run, cancel, mutate]);

  const currentSubmissionState = data?.submissionId === submissionId ? data : null;
  const currentSubmission = currentSubmissionState?.submission ?? null;
  const isLoading = Boolean(submissionId) && (params[0] !== submissionId || loading);
  const requestFailed = Boolean(submissionId) && params[0] === submissionId && !loading && Boolean(error);
  const isPending = isSubmissionPending(currentSubmission);
  const isUnresolved = isLoading || (requestFailed && !currentSubmission);

  let message: string | null = null;
  if (requestFailed) {
    message = options.requestFailedMessage;
  } else if (currentSubmissionState && !currentSubmission && !isLoading) {
    message = options.unavailableMessage;
  }

  function retrySubmissionStatus() {
    if (!submissionId || isLoading) {
      return;
    }

    refresh();
  }

  return {
    submission: currentSubmission,
    isLoading,
    isPending,
    isUnresolved,
    error: message,
    retrySubmissionStatus
  };

  async function loadSubmission(id: string) {
    const response = await fetchQuery<StatusQuery<TResponse>>(environment, options.query, {id}, {
      fetchPolicy: "network-only"
    }).do({
      start: subscription => {
        request.current = subscription;
      }
    }).toPromise();

    if (response === undefined) {
      throw new Error("Submission status request completed without a response");
    }

    return {submissionId: id, submission: options.selectSubmission(response) ?? null};
  }
}

function isSubmissionPending(submission: Submission | null) {
  return submission?.status === "QUEUED" || submission?.status === "RUNNING";
}

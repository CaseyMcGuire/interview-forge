import {graphql} from "react-relay";
import type {useCustomInputSubmissionStatusQuery} from "__generated__/useCustomInputSubmissionStatusQuery.graphql";
import useSubmissionPolling from "./useSubmissionPolling";

const customInputSubmissionStatusQuery = graphql`
  query useCustomInputSubmissionStatusQuery($id: ID!) @throwOnFieldError {
    customInputSubmission(id: $id) {
      id
      status
      ...TestPanel_submission
    }
  }
`;

const pollingOptions = {
  query: customInputSubmissionStatusQuery,
  selectSubmission: (response: useCustomInputSubmissionStatusQuery["response"]) => response.customInputSubmission,
  unavailableMessage: "These test results are unavailable. They may have expired or belong to another account. Run Tests again to get new results.",
  requestFailedMessage: "Couldn't refresh your test results. Retry the status check to see the result."
};

/** Loads per-case feedback through shared polling, with messages that account for expiring results. */
export default function useCustomInputSubmissionStatus(submissionId: string | null) {
  const {retrySubmissionStatus, ...status} = useSubmissionPolling(submissionId, pollingOptions);

  return {
    ...status,
    retryCustomInputSubmissionStatus: retrySubmissionStatus
  };
}

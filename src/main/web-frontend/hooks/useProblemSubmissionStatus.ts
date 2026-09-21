import {graphql} from "react-relay";
import type {useProblemSubmissionStatusQuery} from "__generated__/useProblemSubmissionStatusQuery.graphql";
import useSubmissionPolling from "./useSubmissionPolling";

const problemSubmissionStatusQuery = graphql`
  query useProblemSubmissionStatusQuery($id: ID!) @throwOnFieldError {
    problemSubmission(id: $id) {
      id
      status
      verdict
      ...ProblemSubmissionResultPanel_problemSubmission
    }
  }
`;

const pollingOptions = {
  query: problemSubmissionStatusQuery,
  selectSubmission: (response: useProblemSubmissionStatusQuery["response"]) => response.problemSubmission,
  unavailableMessage: "This submission is unavailable. It may not exist or belong to your signed-in account.",
  requestFailedMessage: "Couldn't refresh your submission. Retry the status check to see its result."
};

/** Loads the problem submission result with its own query and messages through shared polling. */
export default function useProblemSubmissionStatus(problemSubmissionId: string | null) {
  const {submission, retrySubmissionStatus, ...status} = useSubmissionPolling(problemSubmissionId, pollingOptions);

  return {
    ...status,
    problemSubmission: submission,
    retryProblemSubmissionStatus: retrySubmissionStatus
  };
}

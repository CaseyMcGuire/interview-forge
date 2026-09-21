import {useEffect, useRef, useState} from "react";
import {graphql, useMutation} from "react-relay";
import type {Disposable} from "relay-runtime";
import type {useGenerateTestCaseExpectedOutputMutation} from "__generated__/useGenerateTestCaseExpectedOutputMutation.graphql";

/**
 * Generates one answer and exposes progress and API failures. Reset when the input
 * or language changes so an old response cannot overwrite the current draft.
 * Disposing stops listening for the response; server execution may still finish.
 */
export default function useGenerateTestCaseExpectedOutput(onGenerated: (expectedOutputJson: string) => void) {
  const [errors, setErrors] = useState<readonly string[]>([]);
  const [generated, setGenerated] = useState(false);
  const activeRequest = useRef<Disposable | null>(null);
  const [commit, isGenerating] = useMutation<useGenerateTestCaseExpectedOutputMutation>(graphql`
    mutation useGenerateTestCaseExpectedOutputMutation($input: GenerateTestCaseExpectedOutputInput!) {
      generateTestCaseExpectedOutput(input: $input) {
        __typename
        ... on GenerateTestCaseExpectedOutputSuccess {
          expectedOutputJson
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
        ... on ExecutionUnavailable {
          message
        }
        ... on ReferenceSolutionFailed {
          message
        }
      }
    }
  `);

  useEffect(() => () => activeRequest.current?.dispose(), []);

  function generateExpectedOutput(problemLanguageId: string, inputJson: string) {
    if (activeRequest.current) {
      return;
    }

    setErrors([]);
    setGenerated(false);

    activeRequest.current = commit({
      variables: {input: {problemLanguageId, inputJson}},

      onCompleted: (response, graphqlErrors) => {
        activeRequest.current = null;
        const result = response.generateTestCaseExpectedOutput;

        if (graphqlErrors?.length || !result) {
          setErrors(["The expected output could not be generated. Your input is still here; please try again."]);
          return;
        }

        switch (result.__typename) {
          case "GenerateTestCaseExpectedOutputSuccess":
            onGenerated(result.expectedOutputJson);
            setGenerated(true);
            return;

          case "ProblemValidationFailure":
            setErrors(result.fieldErrors.length > 0
              ? result.fieldErrors.map(error => error.message)
              : [result.message]);
            return;

          case "ProblemNotFound":
          case "ProblemForbidden":
          case "ExecutionUnavailable":
          case "ReferenceSolutionFailed":
            setErrors([result.message]);
            return;

          default:
            setErrors(["The expected output could not be generated. Please try again."]);
        }
      },

      onError: () => {
        activeRequest.current = null;
        setErrors(["The request failed. Your input is still here; please try again."]);
      }
    });
  }

  function resetGeneration() {
    activeRequest.current?.dispose();
    activeRequest.current = null;
    setErrors([]);
    setGenerated(false);
  }

  return {generateExpectedOutput, resetGeneration, isGenerating, errors, generated};
}

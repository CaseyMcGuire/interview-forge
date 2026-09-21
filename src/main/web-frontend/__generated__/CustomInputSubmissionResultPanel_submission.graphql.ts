/**
 * @generated SignedSource<<92afdfe1b4ca7efdf11c7b3b97da70fe>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type CustomInputSubmissionOutcome = "COMPILE_ERROR" | "INTERNAL_ERROR" | "INVALID_OUTPUT" | "MEMORY_LIMIT_EXCEEDED" | "OUTPUT_LIMIT_EXCEEDED" | "PASSED" | "REFERENCE_SOLUTION_FAILED" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
export type CustomInputSubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type CustomInputSubmissionResultPanel_submission$data = {
  readonly caseResults: ReadonlyArray<{
    readonly testCase: {
      readonly id: string;
    };
    readonly " $fragmentSpreads": FragmentRefs<"CustomInputSubmissionCaseResult_result">;
  }> | null | undefined;
  readonly outcome: CustomInputSubmissionOutcome | null | undefined;
  readonly passedCases: number;
  readonly publicErrorMessage: string | null | undefined;
  readonly runtimeMs: number | null | undefined;
  readonly status: CustomInputSubmissionStatus;
  readonly totalCases: number;
  readonly " $fragmentType": "CustomInputSubmissionResultPanel_submission";
};
export type CustomInputSubmissionResultPanel_submission$key = {
  readonly " $data"?: CustomInputSubmissionResultPanel_submission$data;
  readonly " $fragmentSpreads": FragmentRefs<"CustomInputSubmissionResultPanel_submission">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "CustomInputSubmissionResultPanel_submission",
  "selections": [
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "status",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "outcome",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "totalCases",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "passedCases",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "runtimeMs",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "publicErrorMessage",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "concreteType": "CustomInputSubmissionCaseResult",
      "kind": "LinkedField",
      "name": "caseResults",
      "plural": true,
      "selections": [
        {
          "alias": null,
          "args": null,
          "concreteType": "CustomTestCase",
          "kind": "LinkedField",
          "name": "testCase",
          "plural": false,
          "selections": [
            {
              "alias": null,
              "args": null,
              "kind": "ScalarField",
              "name": "id",
              "storageKey": null
            }
          ],
          "storageKey": null
        },
        {
          "args": null,
          "kind": "FragmentSpread",
          "name": "CustomInputSubmissionCaseResult_result"
        }
      ],
      "storageKey": null
    }
  ],
  "type": "CustomInputSubmission",
  "abstractKey": null
};

(node as any).hash = "83ec53f0e6988b782309f5262df1e703";

export default node;

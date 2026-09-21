/**
 * @generated SignedSource<<ab2ef6efaa3589fa68707aecbe0feaed>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type CustomInputSubmissionCaseOutcome = "INTERNAL_ERROR" | "INVALID_OUTPUT" | "MEMORY_LIMIT_EXCEEDED" | "NOT_RUN" | "OUTPUT_LIMIT_EXCEEDED" | "PASSED" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
export type CustomInputSubmissionOutcome = "COMPILE_ERROR" | "INTERNAL_ERROR" | "INVALID_OUTPUT" | "MEMORY_LIMIT_EXCEEDED" | "OUTPUT_LIMIT_EXCEEDED" | "PASSED" | "REFERENCE_SOLUTION_FAILED" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
export type CustomInputSubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type TestPanel_submission$data = {
  readonly caseResults: ReadonlyArray<{
    readonly outcome: CustomInputSubmissionCaseOutcome;
    readonly output: string;
    readonly publicErrorMessage: string | null | undefined;
    readonly testCase: {
      readonly expectedOutputJson: string | null | undefined;
      readonly id: string;
      readonly inputJson: string;
      readonly position: number;
    };
  }> | null | undefined;
  readonly outcome: CustomInputSubmissionOutcome | null | undefined;
  readonly passedCases: number;
  readonly publicErrorMessage: string | null | undefined;
  readonly runtimeMs: number | null | undefined;
  readonly status: CustomInputSubmissionStatus;
  readonly totalCases: number;
  readonly " $fragmentType": "TestPanel_submission";
};
export type TestPanel_submission$key = {
  readonly " $data"?: TestPanel_submission$data;
  readonly " $fragmentSpreads": FragmentRefs<"TestPanel_submission">;
};

const node: ReaderFragment = (function(){
var v0 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "outcome",
  "storageKey": null
},
v1 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "publicErrorMessage",
  "storageKey": null
};
return {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "TestPanel_submission",
  "selections": [
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "status",
      "storageKey": null
    },
    (v0/*:: as any*/),
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
    (v1/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "concreteType": "CustomInputSubmissionCaseResult",
      "kind": "LinkedField",
      "name": "caseResults",
      "plural": true,
      "selections": [
        (v0/*:: as any*/),
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "output",
          "storageKey": null
        },
        (v1/*:: as any*/),
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
            },
            {
              "alias": null,
              "args": null,
              "kind": "ScalarField",
              "name": "position",
              "storageKey": null
            },
            {
              "alias": null,
              "args": null,
              "kind": "ScalarField",
              "name": "inputJson",
              "storageKey": null
            },
            {
              "alias": null,
              "args": null,
              "kind": "ScalarField",
              "name": "expectedOutputJson",
              "storageKey": null
            }
          ],
          "storageKey": null
        }
      ],
      "storageKey": null
    }
  ],
  "type": "CustomInputSubmission",
  "abstractKey": null
};
})();

(node as any).hash = "84906b26c9661ead3bd8a8218d2988bf";

export default node;

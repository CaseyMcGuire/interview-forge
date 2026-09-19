/**
 * @generated SignedSource<<e2cc2ee70be195ba0c8dc0b7c148b210>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type SubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
export type SubmissionVerdict = "ACCEPTED" | "COMPILE_ERROR" | "INTERNAL_ERROR" | "MEMORY_LIMIT_EXCEEDED" | "PENDING" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type SubmissionResultPanel_submission$data = {
  readonly failedExample: {
    readonly " $fragmentSpreads": FragmentRefs<"SubmissionFailedExample_example">;
  } | null | undefined;
  readonly passedCases: number;
  readonly publicErrorMessage: string | null | undefined;
  readonly runtimeMs: number | null | undefined;
  readonly status: SubmissionStatus;
  readonly totalCases: number;
  readonly verdict: SubmissionVerdict;
  readonly " $fragmentType": "SubmissionResultPanel_submission";
};
export type SubmissionResultPanel_submission$key = {
  readonly " $data"?: SubmissionResultPanel_submission$data;
  readonly " $fragmentSpreads": FragmentRefs<"SubmissionResultPanel_submission">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "SubmissionResultPanel_submission",
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
      "name": "verdict",
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
      "concreteType": "SubmissionFailedExample",
      "kind": "LinkedField",
      "name": "failedExample",
      "plural": false,
      "selections": [
        {
          "args": null,
          "kind": "FragmentSpread",
          "name": "SubmissionFailedExample_example"
        }
      ],
      "storageKey": null
    }
  ],
  "type": "Submission",
  "abstractKey": null
};

(node as any).hash = "8c36203027c39c73bc2c4afdb84e7fa4";

export default node;

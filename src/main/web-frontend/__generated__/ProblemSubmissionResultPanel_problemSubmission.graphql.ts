/**
 * @generated SignedSource<<daf43a653e5c2e7716b674050c2028b8>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type ProblemSubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
export type ProblemSubmissionVerdict = "ACCEPTED" | "COMPILE_ERROR" | "INTERNAL_ERROR" | "MEMORY_LIMIT_EXCEEDED" | "PENDING" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type ProblemSubmissionResultPanel_problemSubmission$data = {
  readonly failedExample: {
    readonly " $fragmentSpreads": FragmentRefs<"ProblemSubmissionFailedExample_example">;
  } | null | undefined;
  readonly passedCases: number;
  readonly publicErrorMessage: string | null | undefined;
  readonly runtimeMs: number | null | undefined;
  readonly status: ProblemSubmissionStatus;
  readonly totalCases: number;
  readonly verdict: ProblemSubmissionVerdict;
  readonly " $fragmentType": "ProblemSubmissionResultPanel_problemSubmission";
};
export type ProblemSubmissionResultPanel_problemSubmission$key = {
  readonly " $data"?: ProblemSubmissionResultPanel_problemSubmission$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemSubmissionResultPanel_problemSubmission">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemSubmissionResultPanel_problemSubmission",
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
      "concreteType": "ProblemSubmissionFailedExample",
      "kind": "LinkedField",
      "name": "failedExample",
      "plural": false,
      "selections": [
        {
          "args": null,
          "kind": "FragmentSpread",
          "name": "ProblemSubmissionFailedExample_example"
        }
      ],
      "storageKey": null
    }
  ],
  "type": "ProblemSubmission",
  "abstractKey": null
};

(node as any).hash = "8caa888954c409f0672a4c33eec31270";

export default node;

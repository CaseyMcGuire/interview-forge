/**
 * @generated SignedSource<<c0c612bed74c7ba5d2bb84501099b244>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemSubmissionFailedExample_example$data = {
  readonly expectedOutputJson: string;
  readonly inputJson: string;
  readonly output: string;
  readonly " $fragmentType": "ProblemSubmissionFailedExample_example";
};
export type ProblemSubmissionFailedExample_example$key = {
  readonly " $data"?: ProblemSubmissionFailedExample_example$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemSubmissionFailedExample_example">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemSubmissionFailedExample_example",
  "selections": [
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
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "output",
      "storageKey": null
    }
  ],
  "type": "ProblemSubmissionFailedExample",
  "abstractKey": null
};

(node as any).hash = "9371ad801ccc8a6260c1e7b9332cafe7";

export default node;

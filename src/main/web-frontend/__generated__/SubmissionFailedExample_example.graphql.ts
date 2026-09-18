/**
 * @generated SignedSource<<d4aae655c958f1d7a3bee7f0be4e1df3>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type SubmissionFailedExample_example$data = {
  readonly expectedOutputJson: string;
  readonly inputJson: string;
  readonly output: string;
  readonly " $fragmentType": "SubmissionFailedExample_example";
};
export type SubmissionFailedExample_example$key = {
  readonly " $data"?: SubmissionFailedExample_example$data;
  readonly " $fragmentSpreads": FragmentRefs<"SubmissionFailedExample_example">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "SubmissionFailedExample_example",
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
  "type": "SubmissionFailedExample",
  "abstractKey": null
};

(node as any).hash = "f6426085783605d1fc85da18954e736d";

export default node;

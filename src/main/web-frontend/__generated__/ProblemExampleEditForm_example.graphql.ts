/**
 * @generated SignedSource<<9b60db68bd9827bd46ae3a381d672a71>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemExampleEditForm_example$data = {
  readonly expectedOutputJson: string;
  readonly explanationMarkdown: string | null | undefined;
  readonly id: string;
  readonly inputJson: string;
  readonly " $fragmentType": "ProblemExampleEditForm_example";
};
export type ProblemExampleEditForm_example$key = {
  readonly " $data"?: ProblemExampleEditForm_example$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemExampleEditForm_example">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemExampleEditForm_example",
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
      "name": "explanationMarkdown",
      "storageKey": null
    }
  ],
  "type": "ProblemExample",
  "abstractKey": null
};

(node as any).hash = "e6a4f11795f1757dca1214b0ddd4c301";

export default node;

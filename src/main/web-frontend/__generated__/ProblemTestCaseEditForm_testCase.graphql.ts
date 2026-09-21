/**
 * @generated SignedSource<<2245af192822c975868d8db7d4ea1425>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemTestCaseEditForm_testCase$data = {
  readonly expectedOutputJson: string;
  readonly explanationMarkdown: string | null | undefined;
  readonly id: string;
  readonly inputJson: string;
  readonly " $fragmentType": "ProblemTestCaseEditForm_testCase";
};
export type ProblemTestCaseEditForm_testCase$key = {
  readonly " $data"?: ProblemTestCaseEditForm_testCase$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTestCaseEditForm_testCase">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemTestCaseEditForm_testCase",
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
  "type": "ProblemTestCase",
  "abstractKey": null
};

(node as any).hash = "dfde5132db7f537f7e528f391885c4fa";

export default node;

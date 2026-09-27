/**
 * @generated SignedSource<<f0a0feb2f983e3c78237d432973cccfe>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type TestDriverType = "JSON_TEST_DRIVER" | "MAIN_FUNCTION" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type ProblemTestDriverField_language$data = {
  readonly testDriverTypes: ReadonlyArray<{
    readonly displayName: string;
    readonly documentationMarkdown: string;
    readonly type: TestDriverType;
  }>;
  readonly " $fragmentType": "ProblemTestDriverField_language";
};
export type ProblemTestDriverField_language$key = {
  readonly " $data"?: ProblemTestDriverField_language$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTestDriverField_language">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemTestDriverField_language",
  "selections": [
    {
      "alias": null,
      "args": null,
      "concreteType": "TestDriverTypeInfo",
      "kind": "LinkedField",
      "name": "testDriverTypes",
      "plural": true,
      "selections": [
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "type",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "displayName",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "documentationMarkdown",
          "storageKey": null
        }
      ],
      "storageKey": null
    }
  ],
  "type": "Language",
  "abstractKey": null
};

(node as any).hash = "27a81c8a45bfc8924e454ecabf093baa";

export default node;

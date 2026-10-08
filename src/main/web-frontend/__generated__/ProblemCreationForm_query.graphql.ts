/**
 * @generated SignedSource<<e57be63f3c2e3218d65c47c0d1f70a5d>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemCreationForm_query$data = {
  readonly languages: ReadonlyArray<{
    readonly displayName: string;
    readonly id: string;
    readonly key: string;
  }>;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTagField_query">;
  readonly " $fragmentType": "ProblemCreationForm_query";
};
export type ProblemCreationForm_query$key = {
  readonly " $data"?: ProblemCreationForm_query$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemCreationForm_query">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": {
    "throwOnFieldError": true
  },
  "name": "ProblemCreationForm_query",
  "selections": [
    {
      "args": null,
      "kind": "FragmentSpread",
      "name": "ProblemTagField_query"
    },
    {
      "alias": null,
      "args": null,
      "concreteType": "Language",
      "kind": "LinkedField",
      "name": "languages",
      "plural": true,
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
          "name": "key",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "displayName",
          "storageKey": null
        }
      ],
      "storageKey": null
    }
  ],
  "type": "Query",
  "abstractKey": null
};

(node as any).hash = "8516e95ffc4fb4a7940f2dc98876a37c";

export default node;

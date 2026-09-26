/**
 * @generated SignedSource<<935650a2e1b4df28beb2fe27f41a9101>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemTagField_query$data = {
  readonly tags: ReadonlyArray<{
    readonly displayName: string;
    readonly id: string;
    readonly slug: string;
  }>;
  readonly " $fragmentType": "ProblemTagField_query";
};
export type ProblemTagField_query$key = {
  readonly " $data"?: ProblemTagField_query$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTagField_query">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemTagField_query",
  "selections": [
    {
      "alias": null,
      "args": null,
      "concreteType": "Tag",
      "kind": "LinkedField",
      "name": "tags",
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
          "name": "slug",
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

(node as any).hash = "9a9e24b990fb43a009b1f1f792ed57af";

export default node;

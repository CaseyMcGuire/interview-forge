/**
 * @generated SignedSource<<f1e76fc714fb887bf87bae08d7b53367>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type TagType = "COMPANY" | "TOPIC" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type ProblemTagField_query$data = {
  readonly tags: ReadonlyArray<{
    readonly displayName: string;
    readonly id: string;
    readonly slug: string;
    readonly type: TagType;
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
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "type",
          "storageKey": null
        }
      ],
      "storageKey": null
    }
  ],
  "type": "Query",
  "abstractKey": null
};

(node as any).hash = "7c9b31b023c680fb2baa6a7097e1f732";

export default node;

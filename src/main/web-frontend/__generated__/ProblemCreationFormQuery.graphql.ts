/**
 * @generated SignedSource<<5f95631558334bfbf2200c1289371196>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemCreationFormQuery$variables = Record<PropertyKey, never>;
export type ProblemCreationFormQuery$data = {
  readonly languages: ReadonlyArray<{
    readonly displayName: string;
    readonly id: string;
    readonly key: string;
  }>;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTagField_query">;
};
export type ProblemCreationFormQuery = {
  response: ProblemCreationFormQuery$data;
  variables: ProblemCreationFormQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
},
v1 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "displayName",
  "storageKey": null
},
v2 = {
  "alias": null,
  "args": null,
  "concreteType": "Language",
  "kind": "LinkedField",
  "name": "languages",
  "plural": true,
  "selections": [
    (v0/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "key",
      "storageKey": null
    },
    (v1/*:: as any*/)
  ],
  "storageKey": null
};
return {
  "fragment": {
    "argumentDefinitions": [],
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "ProblemCreationFormQuery",
    "selections": [
      {
        "args": null,
        "kind": "FragmentSpread",
        "name": "ProblemTagField_query"
      },
      (v2/*:: as any*/)
    ],
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": [],
    "kind": "Operation",
    "name": "ProblemCreationFormQuery",
    "selections": [
      {
        "alias": null,
        "args": null,
        "concreteType": "Tag",
        "kind": "LinkedField",
        "name": "tags",
        "plural": true,
        "selections": [
          (v0/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "slug",
            "storageKey": null
          },
          (v1/*:: as any*/)
        ],
        "storageKey": null
      },
      (v2/*:: as any*/)
    ]
  },
  "params": {
    "cacheID": "4c0c74c5f64f2dc4be1c6f123a46b36f",
    "id": null,
    "metadata": {},
    "name": "ProblemCreationFormQuery",
    "operationKind": "query",
    "text": "query ProblemCreationFormQuery {\n  ...ProblemTagField_query\n  languages {\n    id\n    key\n    displayName\n  }\n}\n\nfragment ProblemTagField_query on Query {\n  tags {\n    id\n    slug\n    displayName\n  }\n}\n"
  }
};
})();

(node as any).hash = "e5a1384132b44621a9832d814ef820a2";

export default node;

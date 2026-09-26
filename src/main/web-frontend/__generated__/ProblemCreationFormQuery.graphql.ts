/**
 * @generated SignedSource<<f7ac5d786bc577381fa8811080e9cdbd>>
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
          (v1/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "type",
            "storageKey": null
          }
        ],
        "storageKey": null
      },
      (v2/*:: as any*/)
    ]
  },
  "params": {
    "cacheID": "90fb14264b80938aa9acc88980abfb11",
    "id": null,
    "metadata": {},
    "name": "ProblemCreationFormQuery",
    "operationKind": "query",
    "text": "query ProblemCreationFormQuery {\n  ...ProblemTagField_query\n  languages {\n    id\n    key\n    displayName\n  }\n}\n\nfragment ProblemTagField_query on Query {\n  tags {\n    id\n    slug\n    displayName\n    type\n  }\n}\n"
  }
};
})();

(node as any).hash = "e5a1384132b44621a9832d814ef820a2";

export default node;

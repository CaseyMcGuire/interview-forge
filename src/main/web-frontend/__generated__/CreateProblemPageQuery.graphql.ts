/**
 * @generated SignedSource<<f728abe399e32c5b98d3a953021fbbbc>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type CreateProblemPageQuery$variables = Record<PropertyKey, never>;
export type CreateProblemPageQuery$data = {
  readonly " $fragmentSpreads": FragmentRefs<"ProblemCreationForm_query">;
};
export type CreateProblemPageQuery = {
  response: CreateProblemPageQuery$data;
  variables: CreateProblemPageQuery$variables;
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
};
return {
  "fragment": {
    "argumentDefinitions": [],
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "CreateProblemPageQuery",
    "selections": [
      {
        "args": null,
        "kind": "FragmentSpread",
        "name": "ProblemCreationForm_query"
      }
    ],
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": [],
    "kind": "Operation",
    "name": "CreateProblemPageQuery",
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
      {
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
      }
    ]
  },
  "params": {
    "cacheID": "0938142fef5dcdf93514619b5821da52",
    "id": null,
    "metadata": {},
    "name": "CreateProblemPageQuery",
    "operationKind": "query",
    "text": "query CreateProblemPageQuery {\n  ...ProblemCreationForm_query\n}\n\nfragment ProblemCreationForm_query on Query {\n  ...ProblemTagField_query\n  languages {\n    id\n    key\n    displayName\n  }\n}\n\nfragment ProblemTagField_query on Query {\n  tags {\n    id\n    slug\n    displayName\n    type\n  }\n}\n"
  }
};
})();

(node as any).hash = "d951df7595ccc111cddea48576118028";

export default node;

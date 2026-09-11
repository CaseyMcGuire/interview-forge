/**
 * @generated SignedSource<<ee0e31f1e1f2c938fb26c882010ec03d>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemDifficulty = "EASY" | "HARD" | "MEDIUM" | "%future added value";
export type ProblemFilterInput = {
  difficulty?: ProblemDifficulty | null | undefined;
  search?: string | null | undefined;
};
export type ProblemsPageQuery$variables = {
  filters?: ProblemFilterInput | null | undefined;
};
export type ProblemsPageQuery$data = {
  readonly " $fragmentSpreads": FragmentRefs<"ProblemList_query">;
};
export type ProblemsPageQuery = {
  response: ProblemsPageQuery$data;
  variables: ProblemsPageQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "filters"
  }
],
v1 = {
  "kind": "Variable",
  "name": "filters",
  "variableName": "filters"
},
v2 = [
  (v1/*:: as any*/),
  {
    "kind": "Literal",
    "name": "first",
    "value": 20
  }
];
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "ProblemsPageQuery",
    "selections": [
      {
        "args": [
          (v1/*:: as any*/)
        ],
        "kind": "FragmentSpread",
        "name": "ProblemList_query"
      }
    ],
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemsPageQuery",
    "selections": [
      {
        "alias": null,
        "args": (v2/*:: as any*/),
        "concreteType": "ProblemConnection",
        "kind": "LinkedField",
        "name": "problems",
        "plural": false,
        "selections": [
          {
            "alias": null,
            "args": null,
            "concreteType": "ProblemEdge",
            "kind": "LinkedField",
            "name": "edges",
            "plural": true,
            "selections": [
              {
                "alias": null,
                "args": null,
                "concreteType": "Problem",
                "kind": "LinkedField",
                "name": "node",
                "plural": false,
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
                    "name": "title",
                    "storageKey": null
                  },
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "difficulty",
                    "storageKey": null
                  },
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "__typename",
                    "storageKey": null
                  }
                ],
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "cursor",
                "storageKey": null
              }
            ],
            "storageKey": null
          },
          {
            "alias": null,
            "args": null,
            "concreteType": "PageInfo",
            "kind": "LinkedField",
            "name": "pageInfo",
            "plural": false,
            "selections": [
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "endCursor",
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "hasNextPage",
                "storageKey": null
              }
            ],
            "storageKey": null
          }
        ],
        "storageKey": null
      },
      {
        "alias": null,
        "args": (v2/*:: as any*/),
        "filters": [
          "filters"
        ],
        "handle": "connection",
        "key": "ProblemList_problems",
        "kind": "LinkedHandle",
        "name": "problems"
      }
    ]
  },
  "params": {
    "cacheID": "7cfb3da9ae0adf5c8be2786dff7d0e15",
    "id": null,
    "metadata": {},
    "name": "ProblemsPageQuery",
    "operationKind": "query",
    "text": "query ProblemsPageQuery(\n  $filters: ProblemFilterInput\n) {\n  ...ProblemList_query_VTAHT\n}\n\nfragment ProblemList_query_VTAHT on Query {\n  problems(first: 20, filters: $filters) {\n    edges {\n      node {\n        id\n        slug\n        title\n        difficulty\n        __typename\n      }\n      cursor\n    }\n    pageInfo {\n      endCursor\n      hasNextPage\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "22c205af2cb45ab4fe979d76fd2a3382";

export default node;

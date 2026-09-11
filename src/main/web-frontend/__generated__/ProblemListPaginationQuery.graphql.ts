/**
 * @generated SignedSource<<1bb8654a4ab5e1c0e6f60383c108d6e1>>
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
export type ProblemListPaginationQuery$variables = {
  after?: string | null | undefined;
  filters?: ProblemFilterInput | null | undefined;
  first?: number | null | undefined;
};
export type ProblemListPaginationQuery$data = {
  readonly " $fragmentSpreads": FragmentRefs<"ProblemList_query">;
};
export type ProblemListPaginationQuery = {
  response: ProblemListPaginationQuery$data;
  variables: ProblemListPaginationQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "after"
  },
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "filters"
  },
  {
    "defaultValue": 20,
    "kind": "LocalArgument",
    "name": "first"
  }
],
v1 = [
  {
    "kind": "Variable",
    "name": "after",
    "variableName": "after"
  },
  {
    "kind": "Variable",
    "name": "filters",
    "variableName": "filters"
  },
  {
    "kind": "Variable",
    "name": "first",
    "variableName": "first"
  }
];
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": null,
    "name": "ProblemListPaginationQuery",
    "selections": [
      {
        "args": (v1/*:: as any*/),
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
    "name": "ProblemListPaginationQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
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
        "args": (v1/*:: as any*/),
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
    "cacheID": "b77f0a9c8ec215c6de47eab38e4c9afb",
    "id": null,
    "metadata": {},
    "name": "ProblemListPaginationQuery",
    "operationKind": "query",
    "text": "query ProblemListPaginationQuery(\n  $after: String\n  $filters: ProblemFilterInput\n  $first: Int = 20\n) {\n  ...ProblemList_query_1XoDko\n}\n\nfragment ProblemList_query_1XoDko on Query {\n  problems(first: $first, after: $after, filters: $filters) {\n    edges {\n      node {\n        id\n        slug\n        title\n        difficulty\n        __typename\n      }\n      cursor\n    }\n    pageInfo {\n      endCursor\n      hasNextPage\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "2df305031a9ad8bcf002d5cfff9ddf78";

export default node;

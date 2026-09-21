/**
 * @generated SignedSource<<dbc2bf053004932a8edbd03989acea82>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type CreateProblemHiddenTestCasePageQuery$variables = {
  slug: string;
};
export type CreateProblemHiddenTestCasePageQuery$data = {
  readonly problem: {
    readonly id: string;
    readonly slug: string;
    readonly title: string;
  } | null | undefined;
};
export type CreateProblemHiddenTestCasePageQuery = {
  response: CreateProblemHiddenTestCasePageQuery$data;
  variables: CreateProblemHiddenTestCasePageQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "slug"
  }
],
v1 = [
  {
    "alias": null,
    "args": [
      {
        "kind": "Variable",
        "name": "slug",
        "variableName": "slug"
      }
    ],
    "concreteType": "Problem",
    "kind": "LinkedField",
    "name": "problem",
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
      }
    ],
    "storageKey": null
  }
];
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "CreateProblemHiddenTestCasePageQuery",
    "selections": (v1/*:: as any*/),
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "CreateProblemHiddenTestCasePageQuery",
    "selections": (v1/*:: as any*/)
  },
  "params": {
    "cacheID": "728aa5e6c877fbe73e9d930329eda95d",
    "id": null,
    "metadata": {},
    "name": "CreateProblemHiddenTestCasePageQuery",
    "operationKind": "query",
    "text": "query CreateProblemHiddenTestCasePageQuery(\n  $slug: String!\n) {\n  problem(slug: $slug) {\n    id\n    slug\n    title\n  }\n}\n"
  }
};
})();

(node as any).hash = "aeb8f5cfdaa3a5fe6edc7084f7a61361";

export default node;

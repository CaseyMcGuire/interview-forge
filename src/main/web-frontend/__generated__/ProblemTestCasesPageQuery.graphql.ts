/**
 * @generated SignedSource<<262738fd1b0ada41cdd2ff0440396b13>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type ProblemTestCasesPageQuery$variables = {
  slug: string;
};
export type ProblemTestCasesPageQuery$data = {
  readonly problem: {
    readonly slug: string;
    readonly testCases: ReadonlyArray<{
      readonly expectedOutputJson: string;
      readonly id: string;
      readonly inputJson: string;
    }> | null | undefined;
    readonly title: string;
  } | null | undefined;
};
export type ProblemTestCasesPageQuery = {
  response: ProblemTestCasesPageQuery$data;
  variables: ProblemTestCasesPageQuery$variables;
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
    "kind": "Variable",
    "name": "slug",
    "variableName": "slug"
  }
],
v2 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "slug",
  "storageKey": null
},
v3 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "title",
  "storageKey": null
},
v4 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
},
v5 = {
  "alias": null,
  "args": null,
  "concreteType": "ProblemTestCase",
  "kind": "LinkedField",
  "name": "testCases",
  "plural": true,
  "selections": [
    (v4/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "inputJson",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "expectedOutputJson",
      "storageKey": null
    }
  ],
  "storageKey": null
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "ProblemTestCasesPageQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "Problem",
        "kind": "LinkedField",
        "name": "problem",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          (v5/*:: as any*/)
        ],
        "storageKey": null
      }
    ],
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemTestCasesPageQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "Problem",
        "kind": "LinkedField",
        "name": "problem",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          (v5/*:: as any*/),
          (v4/*:: as any*/)
        ],
        "storageKey": null
      }
    ]
  },
  "params": {
    "cacheID": "87f55ea80c66257691e5c467a5754860",
    "id": null,
    "metadata": {},
    "name": "ProblemTestCasesPageQuery",
    "operationKind": "query",
    "text": "query ProblemTestCasesPageQuery(\n  $slug: String!\n) {\n  problem(slug: $slug) {\n    slug\n    title\n    testCases {\n      id\n      inputJson\n      expectedOutputJson\n    }\n    id\n  }\n}\n"
  }
};
})();

(node as any).hash = "7793bf9e2e0d230d47f8c8f27509820e";

export default node;

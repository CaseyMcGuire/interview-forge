/**
 * @generated SignedSource<<1dc2594d0ad505357fa54bbec35e6b1a>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type EditProblemTestCasePageQuery$variables = {
  id: string;
  slug: string;
};
export type EditProblemTestCasePageQuery$data = {
  readonly problem: {
    readonly slug: string;
    readonly testCase: {
      readonly id: string;
      readonly " $fragmentSpreads": FragmentRefs<"ProblemTestCaseEditForm_testCase">;
    } | null | undefined;
    readonly title: string;
    readonly " $fragmentSpreads": FragmentRefs<"ProblemTestCaseEditForm_problem">;
  } | null | undefined;
};
export type EditProblemTestCasePageQuery = {
  response: EditProblemTestCasePageQuery$data;
  variables: EditProblemTestCasePageQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = {
  "defaultValue": null,
  "kind": "LocalArgument",
  "name": "id"
},
v1 = {
  "defaultValue": null,
  "kind": "LocalArgument",
  "name": "slug"
},
v2 = [
  {
    "kind": "Variable",
    "name": "slug",
    "variableName": "slug"
  }
],
v3 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "slug",
  "storageKey": null
},
v4 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "title",
  "storageKey": null
},
v5 = [
  {
    "kind": "Variable",
    "name": "id",
    "variableName": "id"
  }
],
v6 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
};
return {
  "fragment": {
    "argumentDefinitions": [
      (v0/*:: as any*/),
      (v1/*:: as any*/)
    ],
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "EditProblemTestCasePageQuery",
    "selections": [
      {
        "alias": null,
        "args": (v2/*:: as any*/),
        "concreteType": "Problem",
        "kind": "LinkedField",
        "name": "problem",
        "plural": false,
        "selections": [
          (v3/*:: as any*/),
          (v4/*:: as any*/),
          {
            "args": null,
            "kind": "FragmentSpread",
            "name": "ProblemTestCaseEditForm_problem"
          },
          {
            "alias": null,
            "args": (v5/*:: as any*/),
            "concreteType": "ProblemTestCase",
            "kind": "LinkedField",
            "name": "testCase",
            "plural": false,
            "selections": [
              (v6/*:: as any*/),
              {
                "args": null,
                "kind": "FragmentSpread",
                "name": "ProblemTestCaseEditForm_testCase"
              }
            ],
            "storageKey": null
          }
        ],
        "storageKey": null
      }
    ],
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": [
      (v1/*:: as any*/),
      (v0/*:: as any*/)
    ],
    "kind": "Operation",
    "name": "EditProblemTestCasePageQuery",
    "selections": [
      {
        "alias": null,
        "args": (v2/*:: as any*/),
        "concreteType": "Problem",
        "kind": "LinkedField",
        "name": "problem",
        "plural": false,
        "selections": [
          (v3/*:: as any*/),
          (v4/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "concreteType": "ProblemLanguage",
            "kind": "LinkedField",
            "name": "languageConfigurations",
            "plural": true,
            "selections": [
              (v6/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "concreteType": "Language",
                "kind": "LinkedField",
                "name": "language",
                "plural": false,
                "selections": [
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "displayName",
                    "storageKey": null
                  },
                  (v6/*:: as any*/)
                ],
                "storageKey": null
              }
            ],
            "storageKey": null
          },
          {
            "alias": null,
            "args": (v5/*:: as any*/),
            "concreteType": "ProblemTestCase",
            "kind": "LinkedField",
            "name": "testCase",
            "plural": false,
            "selections": [
              (v6/*:: as any*/),
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
              },
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "explanationMarkdown",
                "storageKey": null
              }
            ],
            "storageKey": null
          },
          (v6/*:: as any*/)
        ],
        "storageKey": null
      }
    ]
  },
  "params": {
    "cacheID": "a4f9c8c55a7192c936e49e2f97a6e442",
    "id": null,
    "metadata": {},
    "name": "EditProblemTestCasePageQuery",
    "operationKind": "query",
    "text": "query EditProblemTestCasePageQuery(\n  $slug: String!\n  $id: ID!\n) {\n  problem(slug: $slug) {\n    slug\n    title\n    ...ProblemTestCaseEditForm_problem\n    testCase(id: $id) {\n      id\n      ...ProblemTestCaseEditForm_testCase\n    }\n    id\n  }\n}\n\nfragment ProblemTestCaseEditForm_problem on Problem {\n  languageConfigurations {\n    id\n    language {\n      displayName\n      id\n    }\n  }\n}\n\nfragment ProblemTestCaseEditForm_testCase on ProblemTestCase {\n  id\n  inputJson\n  expectedOutputJson\n  explanationMarkdown\n}\n"
  }
};
})();

(node as any).hash = "e5caa40a5f3c27a67b856a799aa8d945";

export default node;

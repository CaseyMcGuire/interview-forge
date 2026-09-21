/**
 * @generated SignedSource<<b6daff932680d00ab44a90cd8e91d184>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type CreateProblemTestCasePageQuery$variables = {
  slug: string;
};
export type CreateProblemTestCasePageQuery$data = {
  readonly problem: {
    readonly id: string;
    readonly slug: string;
    readonly title: string;
    readonly " $fragmentSpreads": FragmentRefs<"ProblemTestCaseCreationForm_problem">;
  } | null | undefined;
};
export type CreateProblemTestCasePageQuery = {
  response: CreateProblemTestCasePageQuery$data;
  variables: CreateProblemTestCasePageQuery$variables;
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
  "name": "id",
  "storageKey": null
},
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
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "CreateProblemTestCasePageQuery",
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
          (v4/*:: as any*/),
          {
            "args": null,
            "kind": "FragmentSpread",
            "name": "ProblemTestCaseCreationForm_problem"
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
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "CreateProblemTestCasePageQuery",
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
          (v4/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "concreteType": "ProblemLanguage",
            "kind": "LinkedField",
            "name": "languageConfigurations",
            "plural": true,
            "selections": [
              (v2/*:: as any*/),
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
                  (v2/*:: as any*/)
                ],
                "storageKey": null
              }
            ],
            "storageKey": null
          }
        ],
        "storageKey": null
      }
    ]
  },
  "params": {
    "cacheID": "8ace0f3c2f90150f1a4a33183b03a5fc",
    "id": null,
    "metadata": {},
    "name": "CreateProblemTestCasePageQuery",
    "operationKind": "query",
    "text": "query CreateProblemTestCasePageQuery(\n  $slug: String!\n) {\n  problem(slug: $slug) {\n    id\n    slug\n    title\n    ...ProblemTestCaseCreationForm_problem\n  }\n}\n\nfragment ProblemTestCaseCreationForm_problem on Problem {\n  id\n  languageConfigurations {\n    id\n    language {\n      displayName\n      id\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "77be66b267bc6b02ffc65a383ba5688a";

export default node;

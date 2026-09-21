/**
 * @generated SignedSource<<13222fb9948ee37027b9f914231b2421>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type CreateProblemHiddenTestCasePageQuery$variables = {
  slug: string;
};
export type CreateProblemHiddenTestCasePageQuery$data = {
  readonly problem: {
    readonly id: string;
    readonly slug: string;
    readonly title: string;
    readonly " $fragmentSpreads": FragmentRefs<"ProblemHiddenTestCaseCreationForm_problem">;
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
    "name": "CreateProblemHiddenTestCasePageQuery",
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
            "name": "ProblemHiddenTestCaseCreationForm_problem"
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
    "name": "CreateProblemHiddenTestCasePageQuery",
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
    "cacheID": "65fc441e291711869c222afb09fa1df2",
    "id": null,
    "metadata": {},
    "name": "CreateProblemHiddenTestCasePageQuery",
    "operationKind": "query",
    "text": "query CreateProblemHiddenTestCasePageQuery(\n  $slug: String!\n) {\n  problem(slug: $slug) {\n    id\n    slug\n    title\n    ...ProblemHiddenTestCaseCreationForm_problem\n  }\n}\n\nfragment ProblemHiddenTestCaseCreationForm_problem on Problem {\n  id\n  languageConfigurations {\n    id\n    language {\n      displayName\n      id\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "81ed2b5e256e21c813b1d63085590e8c";

export default node;

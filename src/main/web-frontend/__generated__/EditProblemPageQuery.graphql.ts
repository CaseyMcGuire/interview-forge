/**
 * @generated SignedSource<<d7e1de685833562f55c486d539653b49>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type EditProblemPageQuery$variables = {
  slug: string;
};
export type EditProblemPageQuery$data = {
  readonly problem: {
    readonly examples: ReadonlyArray<{
      readonly id: string;
      readonly " $fragmentSpreads": FragmentRefs<"ProblemExampleEditForm_example">;
    }>;
    readonly id: string;
    readonly languageConfigurations: ReadonlyArray<{
      readonly id: string;
      readonly " $fragmentSpreads": FragmentRefs<"ProblemLanguageEditForm_configuration">;
    }>;
    readonly slug: string;
    readonly " $fragmentSpreads": FragmentRefs<"ProblemDetailsForm_problem">;
  } | null | undefined;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTagField_query">;
};
export type EditProblemPageQuery = {
  response: EditProblemPageQuery$data;
  variables: EditProblemPageQuery$variables;
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
  "name": "displayName",
  "storageKey": null
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "EditProblemPageQuery",
    "selections": [
      {
        "args": null,
        "kind": "FragmentSpread",
        "name": "ProblemTagField_query"
      },
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
          {
            "args": null,
            "kind": "FragmentSpread",
            "name": "ProblemDetailsForm_problem"
          },
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
                "args": null,
                "kind": "FragmentSpread",
                "name": "ProblemLanguageEditForm_configuration"
              }
            ],
            "storageKey": null
          },
          {
            "alias": null,
            "args": null,
            "concreteType": "ProblemExample",
            "kind": "LinkedField",
            "name": "examples",
            "plural": true,
            "selections": [
              (v2/*:: as any*/),
              {
                "args": null,
                "kind": "FragmentSpread",
                "name": "ProblemExampleEditForm_example"
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
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "EditProblemPageQuery",
    "selections": [
      {
        "alias": null,
        "args": null,
        "concreteType": "Tag",
        "kind": "LinkedField",
        "name": "tags",
        "plural": true,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          (v4/*:: as any*/)
        ],
        "storageKey": null
      },
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
            "name": "statementMarkdown",
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
            "concreteType": "Tag",
            "kind": "LinkedField",
            "name": "tags",
            "plural": true,
            "selections": [
              (v2/*:: as any*/)
            ],
            "storageKey": null
          },
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
                "kind": "ScalarField",
                "name": "starterCode",
                "storageKey": null
              },
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
                    "name": "key",
                    "storageKey": null
                  },
                  (v4/*:: as any*/),
                  (v2/*:: as any*/)
                ],
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "concreteType": "JudgeConfiguration",
                "kind": "LinkedField",
                "name": "judgeConfiguration",
                "plural": false,
                "selections": [
                  (v2/*:: as any*/),
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "testDriverCode",
                    "storageKey": null
                  },
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "checkerSource",
                    "storageKey": null
                  },
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "timeLimitMs",
                    "storageKey": null
                  },
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "memoryLimitMb",
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
            "args": null,
            "concreteType": "ProblemExample",
            "kind": "LinkedField",
            "name": "examples",
            "plural": true,
            "selections": [
              (v2/*:: as any*/),
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
          }
        ],
        "storageKey": null
      }
    ]
  },
  "params": {
    "cacheID": "967c92239611543c6c3e2975973c16f0",
    "id": null,
    "metadata": {},
    "name": "EditProblemPageQuery",
    "operationKind": "query",
    "text": "query EditProblemPageQuery(\n  $slug: String!\n) {\n  ...ProblemTagField_query\n  problem(slug: $slug) {\n    id\n    slug\n    ...ProblemDetailsForm_problem\n    languageConfigurations {\n      id\n      ...ProblemLanguageEditForm_configuration\n    }\n    examples {\n      id\n      ...ProblemExampleEditForm_example\n    }\n  }\n}\n\nfragment ProblemDetailsForm_problem on Problem {\n  id\n  title\n  statementMarkdown\n  difficulty\n  tags {\n    id\n  }\n}\n\nfragment ProblemExampleEditForm_example on ProblemExample {\n  id\n  inputJson\n  expectedOutputJson\n  explanationMarkdown\n}\n\nfragment ProblemJudgeConfigurationEditor_configuration on ProblemLanguage {\n  id\n  language {\n    key\n    displayName\n    id\n  }\n  judgeConfiguration {\n    id\n    testDriverCode\n    checkerSource\n    timeLimitMs\n    memoryLimitMb\n  }\n}\n\nfragment ProblemLanguageEditForm_configuration on ProblemLanguage {\n  id\n  starterCode\n  language {\n    key\n    displayName\n    id\n  }\n  ...ProblemJudgeConfigurationEditor_configuration\n}\n\nfragment ProblemTagField_query on Query {\n  tags {\n    id\n    slug\n    displayName\n  }\n}\n"
  }
};
})();

(node as any).hash = "9231737605cd6d3db626c6cf712e1e0d";

export default node;

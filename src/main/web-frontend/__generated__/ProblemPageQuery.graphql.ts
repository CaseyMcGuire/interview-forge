/**
 * @generated SignedSource<<ed9d08193f3389af0236cbcf74c648c9>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type ProblemDifficulty = "EASY" | "HARD" | "MEDIUM" | "%future added value";
export type ProblemPageQuery$variables = {
  slug: string;
};
export type ProblemPageQuery$data = {
  readonly problem: {
    readonly canEdit: boolean;
    readonly difficulty: ProblemDifficulty;
    readonly examples: ReadonlyArray<{
      readonly expectedOutputJson: string;
      readonly explanationMarkdown: string | null | undefined;
      readonly id: string;
      readonly inputJson: string;
      readonly position: number;
    }>;
    readonly id: string;
    readonly languageConfigurations: ReadonlyArray<{
      readonly id: string;
      readonly language: {
        readonly displayName: string;
        readonly id: string;
        readonly key: string;
      };
      readonly starterCode: string;
    }>;
    readonly slug: string;
    readonly statementMarkdown: string;
    readonly tags: ReadonlyArray<{
      readonly displayName: string;
      readonly id: string;
    }>;
    readonly title: string;
  } | null | undefined;
};
export type ProblemPageQuery = {
  response: ProblemPageQuery$data;
  variables: ProblemPageQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "slug"
  }
],
v1 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
},
v2 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "displayName",
  "storageKey": null
},
v3 = [
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
      (v1/*:: as any*/),
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
        "kind": "ScalarField",
        "name": "canEdit",
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
          (v1/*:: as any*/),
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
          (v1/*:: as any*/),
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
              (v1/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "key",
                "storageKey": null
              },
              (v2/*:: as any*/)
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
          (v1/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "position",
            "storageKey": null
          },
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
];
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "ProblemPageQuery",
    "selections": (v3/*:: as any*/),
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemPageQuery",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "1dc636b60c099bdcfde6d3ed0a374ac7",
    "id": null,
    "metadata": {},
    "name": "ProblemPageQuery",
    "operationKind": "query",
    "text": "query ProblemPageQuery(\n  $slug: String!\n) {\n  problem(slug: $slug) {\n    id\n    slug\n    title\n    statementMarkdown\n    difficulty\n    canEdit\n    tags {\n      id\n      displayName\n    }\n    languageConfigurations {\n      id\n      starterCode\n      language {\n        id\n        key\n        displayName\n      }\n    }\n    examples {\n      id\n      position\n      inputJson\n      expectedOutputJson\n      explanationMarkdown\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "3e298f74854b830c4dd40dc4833f7473";

export default node;

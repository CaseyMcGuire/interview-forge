/**
 * @generated SignedSource<<7cd40283e7dc01d0bac95454d456751c>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type ProblemDifficulty = "EASY" | "HARD" | "MEDIUM" | "%future added value";
export type UpdateProblemInput = {
  difficulty: ProblemDifficulty;
  examples: ReadonlyArray<UpdateProblemExampleInput>;
  languageConfigurations: ReadonlyArray<UpdateProblemLanguageInput>;
  statementMarkdown: string;
  title: string;
};
export type UpdateProblemLanguageInput = {
  id: string;
  solutionFilename: string;
  starterCode: string;
};
export type UpdateProblemExampleInput = {
  expectedOutputJson: string;
  explanationMarkdown?: string | null | undefined;
  id: string;
  inputJson: string;
};
export type ProblemEditFormMutation$variables = {
  input: UpdateProblemInput;
  slug: string;
};
export type ProblemEditFormMutation$data = {
  readonly updateProblem: {
    readonly __typename: string;
    readonly message?: string;
    readonly problem?: {
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
        readonly solutionFilename: string;
        readonly starterCode: string;
      }>;
      readonly slug: string;
      readonly statementMarkdown: string;
      readonly title: string;
    };
  };
};
export type ProblemEditFormMutation = {
  response: ProblemEditFormMutation$data;
  variables: ProblemEditFormMutation$variables;
};

const node: ConcreteRequest = (function(){
var v0 = {
  "defaultValue": null,
  "kind": "LocalArgument",
  "name": "input"
},
v1 = {
  "defaultValue": null,
  "kind": "LocalArgument",
  "name": "slug"
},
v2 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
},
v3 = [
  {
    "alias": null,
    "args": [
      {
        "kind": "Variable",
        "name": "input",
        "variableName": "input"
      },
      {
        "kind": "Variable",
        "name": "slug",
        "variableName": "slug"
      }
    ],
    "concreteType": null,
    "kind": "LinkedField",
    "name": "updateProblem",
    "plural": false,
    "selections": [
      {
        "alias": null,
        "args": null,
        "kind": "ScalarField",
        "name": "__typename",
        "storageKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": [
          {
            "alias": null,
            "args": null,
            "concreteType": "Problem",
            "kind": "LinkedField",
            "name": "problem",
            "plural": false,
            "selections": [
              (v2/*:: as any*/),
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
                    "kind": "ScalarField",
                    "name": "solutionFilename",
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
                      (v2/*:: as any*/),
                      {
                        "alias": null,
                        "args": null,
                        "kind": "ScalarField",
                        "name": "key",
                        "storageKey": null
                      },
                      {
                        "alias": null,
                        "args": null,
                        "kind": "ScalarField",
                        "name": "displayName",
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
        ],
        "type": "UpdateProblemSuccess",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": [
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "message",
            "storageKey": null
          }
        ],
        "type": "UpdateProblemFailure",
        "abstractKey": "__isUpdateProblemFailure"
      }
    ],
    "storageKey": null
  }
];
return {
  "fragment": {
    "argumentDefinitions": [
      (v0/*:: as any*/),
      (v1/*:: as any*/)
    ],
    "kind": "Fragment",
    "metadata": null,
    "name": "ProblemEditFormMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": [
      (v1/*:: as any*/),
      (v0/*:: as any*/)
    ],
    "kind": "Operation",
    "name": "ProblemEditFormMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "8802caa42618c8725a1fa89b32d0c058",
    "id": null,
    "metadata": {},
    "name": "ProblemEditFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemEditFormMutation(\n  $slug: String!\n  $input: UpdateProblemInput!\n) {\n  updateProblem(slug: $slug, input: $input) {\n    __typename\n    ... on UpdateProblemSuccess {\n      problem {\n        id\n        slug\n        title\n        statementMarkdown\n        difficulty\n        languageConfigurations {\n          id\n          starterCode\n          solutionFilename\n          language {\n            id\n            key\n            displayName\n          }\n        }\n        examples {\n          id\n          position\n          inputJson\n          expectedOutputJson\n          explanationMarkdown\n        }\n      }\n    }\n    ... on UpdateProblemFailure {\n      __isUpdateProblemFailure: __typename\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "0b5c69e49c06b375a2bd42fe54ae4f5f";

export default node;

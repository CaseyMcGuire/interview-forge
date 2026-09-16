/**
 * @generated SignedSource<<7699a3bec77b5540a19eeeb976af13c5>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type CreateJudgeConfigurationInput = {
  checkerSource?: string | null | undefined;
  memoryLimitMb: number;
  problemLanguageId: string;
  runtime: string;
  testDriverCode: string;
  timeLimitMs: number;
};
export type ProblemJudgeConfigurationEditorCreateMutation$variables = {
  input: CreateJudgeConfigurationInput;
};
export type ProblemJudgeConfigurationEditorCreateMutation$data = {
  readonly createJudgeConfiguration: {
    readonly __typename: "CreateJudgeConfigurationSuccess";
    readonly problemLanguage: {
      readonly id: string;
      readonly judgeConfiguration: {
        readonly checkerSource: string | null | undefined;
        readonly id: string;
        readonly memoryLimitMb: number;
        readonly runtime: string;
        readonly testDriverCode: string;
        readonly timeLimitMs: number;
      } | null | undefined;
    };
  } | {
    readonly __typename: "ProblemForbidden";
    readonly message: string;
  } | {
    readonly __typename: "ProblemNotFound";
    readonly message: string;
  } | {
    readonly __typename: "ProblemValidationFailure";
    readonly fieldErrors: ReadonlyArray<{
      readonly message: string;
    }>;
    readonly message: string;
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type ProblemJudgeConfigurationEditorCreateMutation = {
  response: ProblemJudgeConfigurationEditorCreateMutation$data;
  variables: ProblemJudgeConfigurationEditorCreateMutation$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "input"
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
  "name": "message",
  "storageKey": null
},
v3 = [
  (v2/*:: as any*/)
],
v4 = [
  {
    "alias": null,
    "args": [
      {
        "kind": "Variable",
        "name": "input",
        "variableName": "input"
      }
    ],
    "concreteType": null,
    "kind": "LinkedField",
    "name": "createJudgeConfiguration",
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
            "concreteType": "ProblemLanguage",
            "kind": "LinkedField",
            "name": "problemLanguage",
            "plural": false,
            "selections": [
              (v1/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "concreteType": "JudgeConfiguration",
                "kind": "LinkedField",
                "name": "judgeConfiguration",
                "plural": false,
                "selections": [
                  (v1/*:: as any*/),
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "runtime",
                    "storageKey": null
                  },
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
          }
        ],
        "type": "CreateJudgeConfigurationSuccess",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": [
          (v2/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "concreteType": "FieldError",
            "kind": "LinkedField",
            "name": "fieldErrors",
            "plural": true,
            "selections": (v3/*:: as any*/),
            "storageKey": null
          }
        ],
        "type": "ProblemValidationFailure",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v3/*:: as any*/),
        "type": "ProblemNotFound",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v3/*:: as any*/),
        "type": "ProblemForbidden",
        "abstractKey": null
      }
    ],
    "storageKey": null
  }
];
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": null,
    "name": "ProblemJudgeConfigurationEditorCreateMutation",
    "selections": (v4/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemJudgeConfigurationEditorCreateMutation",
    "selections": (v4/*:: as any*/)
  },
  "params": {
    "cacheID": "482abfd53a7ba3a5248e3863c8e5a95a",
    "id": null,
    "metadata": {},
    "name": "ProblemJudgeConfigurationEditorCreateMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemJudgeConfigurationEditorCreateMutation(\n  $input: CreateJudgeConfigurationInput!\n) {\n  createJudgeConfiguration(input: $input) {\n    __typename\n    ... on CreateJudgeConfigurationSuccess {\n      problemLanguage {\n        id\n        judgeConfiguration {\n          id\n          runtime\n          testDriverCode\n          checkerSource\n          timeLimitMs\n          memoryLimitMb\n        }\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "8c3c97789587b83e7db7605812e923b1";

export default node;

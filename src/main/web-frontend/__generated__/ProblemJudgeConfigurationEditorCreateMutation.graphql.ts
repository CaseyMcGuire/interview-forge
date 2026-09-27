/**
 * @generated SignedSource<<1a88f53773c70f313febc79aca0d0879>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type TestDriverType = "JSON_TEST_DRIVER" | "MAIN_FUNCTION" | "%future added value";
export type CreateJudgeConfigurationInput = {
  memoryLimitMb: number;
  problemLanguageId: string;
  referenceSolutionCode?: string | null | undefined;
  testDriverCode: string;
  testDriverType?: TestDriverType;
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
        readonly id: string;
        readonly memoryLimitMb: number;
        readonly testDriverCode: string;
        readonly testDriverType: TestDriverType;
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
                    "name": "testDriverType",
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
    "cacheID": "73d1008717267c8b6b27107694ba05c6",
    "id": null,
    "metadata": {},
    "name": "ProblemJudgeConfigurationEditorCreateMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemJudgeConfigurationEditorCreateMutation(\n  $input: CreateJudgeConfigurationInput!\n) {\n  createJudgeConfiguration(input: $input) {\n    __typename\n    ... on CreateJudgeConfigurationSuccess {\n      problemLanguage {\n        id\n        judgeConfiguration {\n          id\n          testDriverType\n          testDriverCode\n          timeLimitMs\n          memoryLimitMb\n        }\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "792b44bb96a6bf116bab03b3357e0750";

export default node;

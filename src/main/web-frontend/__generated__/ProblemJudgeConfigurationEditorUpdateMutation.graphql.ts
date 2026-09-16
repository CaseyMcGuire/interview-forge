/**
 * @generated SignedSource<<3fd987e825e167a9e31c30071665bc86>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type UpdateJudgeConfigurationInput = {
  checkerSource?: string | null | undefined;
  id: string;
  memoryLimitMb?: number | null | undefined;
  runtime?: string | null | undefined;
  testDriverCode?: string | null | undefined;
  timeLimitMs?: number | null | undefined;
};
export type ProblemJudgeConfigurationEditorUpdateMutation$variables = {
  input: UpdateJudgeConfigurationInput;
};
export type ProblemJudgeConfigurationEditorUpdateMutation$data = {
  readonly updateJudgeConfiguration: {
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
    readonly __typename: "UpdateJudgeConfigurationSuccess";
    readonly judgeConfiguration: {
      readonly checkerSource: string | null | undefined;
      readonly id: string;
      readonly memoryLimitMb: number;
      readonly runtime: string;
      readonly testDriverCode: string;
      readonly timeLimitMs: number;
    };
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type ProblemJudgeConfigurationEditorUpdateMutation = {
  response: ProblemJudgeConfigurationEditorUpdateMutation$data;
  variables: ProblemJudgeConfigurationEditorUpdateMutation$variables;
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
  "name": "message",
  "storageKey": null
},
v2 = [
  (v1/*:: as any*/)
],
v3 = [
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
    "name": "updateJudgeConfiguration",
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
            "concreteType": "JudgeConfiguration",
            "kind": "LinkedField",
            "name": "judgeConfiguration",
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
        "type": "UpdateJudgeConfigurationSuccess",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": [
          (v1/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "concreteType": "FieldError",
            "kind": "LinkedField",
            "name": "fieldErrors",
            "plural": true,
            "selections": (v2/*:: as any*/),
            "storageKey": null
          }
        ],
        "type": "ProblemValidationFailure",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v2/*:: as any*/),
        "type": "ProblemNotFound",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v2/*:: as any*/),
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
    "name": "ProblemJudgeConfigurationEditorUpdateMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemJudgeConfigurationEditorUpdateMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "b3e57876df9ce6a5a39bfa78ba14bcb4",
    "id": null,
    "metadata": {},
    "name": "ProblemJudgeConfigurationEditorUpdateMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemJudgeConfigurationEditorUpdateMutation(\n  $input: UpdateJudgeConfigurationInput!\n) {\n  updateJudgeConfiguration(input: $input) {\n    __typename\n    ... on UpdateJudgeConfigurationSuccess {\n      judgeConfiguration {\n        id\n        runtime\n        testDriverCode\n        checkerSource\n        timeLimitMs\n        memoryLimitMb\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "b6d28e3acb17c5f41d5d9e313fbeae51";

export default node;

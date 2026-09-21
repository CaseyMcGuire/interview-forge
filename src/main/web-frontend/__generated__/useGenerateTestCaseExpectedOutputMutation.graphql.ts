/**
 * @generated SignedSource<<f6300ac7377c766a157abae0677e9995>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type GenerateTestCaseExpectedOutputInput = {
  inputJson: string;
  problemLanguageId: string;
};
export type useGenerateTestCaseExpectedOutputMutation$variables = {
  input: GenerateTestCaseExpectedOutputInput;
};
export type useGenerateTestCaseExpectedOutputMutation$data = {
  readonly generateTestCaseExpectedOutput: {
    readonly __typename: "ExecutionUnavailable";
    readonly message: string;
  } | {
    readonly __typename: "GenerateTestCaseExpectedOutputSuccess";
    readonly expectedOutputJson: string;
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
    readonly __typename: "ReferenceSolutionFailed";
    readonly message: string;
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type useGenerateTestCaseExpectedOutputMutation = {
  response: useGenerateTestCaseExpectedOutputMutation$data;
  variables: useGenerateTestCaseExpectedOutputMutation$variables;
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
    "name": "generateTestCaseExpectedOutput",
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
            "kind": "ScalarField",
            "name": "expectedOutputJson",
            "storageKey": null
          }
        ],
        "type": "GenerateTestCaseExpectedOutputSuccess",
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
      },
      {
        "kind": "InlineFragment",
        "selections": (v2/*:: as any*/),
        "type": "ExecutionUnavailable",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v2/*:: as any*/),
        "type": "ReferenceSolutionFailed",
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
    "name": "useGenerateTestCaseExpectedOutputMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "useGenerateTestCaseExpectedOutputMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "608d513dd4537b828711743065923547",
    "id": null,
    "metadata": {},
    "name": "useGenerateTestCaseExpectedOutputMutation",
    "operationKind": "mutation",
    "text": "mutation useGenerateTestCaseExpectedOutputMutation(\n  $input: GenerateTestCaseExpectedOutputInput!\n) {\n  generateTestCaseExpectedOutput(input: $input) {\n    __typename\n    ... on GenerateTestCaseExpectedOutputSuccess {\n      expectedOutputJson\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n    ... on ExecutionUnavailable {\n      message\n    }\n    ... on ReferenceSolutionFailed {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "ccbe4d659800b3aa63047b7b7214520c";

export default node;

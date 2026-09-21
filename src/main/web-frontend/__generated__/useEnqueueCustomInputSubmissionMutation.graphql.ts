/**
 * @generated SignedSource<<38a3935f4a8936c2fbc117922e437654>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type EnqueueCustomInputSubmissionInput = {
  cases: ReadonlyArray<CustomTestCaseInput>;
  problemLanguageId: string;
  sourceCode: string;
};
export type CustomTestCaseInput = {
  inputJson: string;
};
export type useEnqueueCustomInputSubmissionMutation$variables = {
  input: EnqueueCustomInputSubmissionInput;
};
export type useEnqueueCustomInputSubmissionMutation$data = {
  readonly enqueueCustomInputSubmission: {
    readonly __typename: "AuthenticationRequired";
    readonly message: string;
  } | {
    readonly __typename: "EnqueueCustomInputSubmissionSuccess";
    readonly customInputSubmissionId: string;
  } | {
    readonly __typename: "EnqueueCustomInputSubmissionValidationFailure";
    readonly fieldErrors: ReadonlyArray<{
      readonly field: string;
      readonly message: string;
    }>;
    readonly message: string;
  } | {
    readonly __typename: "ExecutionBusy";
    readonly message: string;
  } | {
    readonly __typename: "ExecutionUnavailable";
    readonly message: string;
  } | {
    readonly __typename: "ProblemNotFound";
    readonly message: string;
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type useEnqueueCustomInputSubmissionMutation = {
  response: useEnqueueCustomInputSubmissionMutation$data;
  variables: useEnqueueCustomInputSubmissionMutation$variables;
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
    "name": "enqueueCustomInputSubmission",
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
            "name": "customInputSubmissionId",
            "storageKey": null
          }
        ],
        "type": "EnqueueCustomInputSubmissionSuccess",
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
            "selections": [
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "field",
                "storageKey": null
              },
              (v1/*:: as any*/)
            ],
            "storageKey": null
          }
        ],
        "type": "EnqueueCustomInputSubmissionValidationFailure",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v2/*:: as any*/),
        "type": "AuthenticationRequired",
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
        "type": "ExecutionUnavailable",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v2/*:: as any*/),
        "type": "ExecutionBusy",
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
    "name": "useEnqueueCustomInputSubmissionMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "useEnqueueCustomInputSubmissionMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "eadc4625d01b9ec02132d1c5ee615460",
    "id": null,
    "metadata": {},
    "name": "useEnqueueCustomInputSubmissionMutation",
    "operationKind": "mutation",
    "text": "mutation useEnqueueCustomInputSubmissionMutation(\n  $input: EnqueueCustomInputSubmissionInput!\n) {\n  enqueueCustomInputSubmission(input: $input) {\n    __typename\n    ... on EnqueueCustomInputSubmissionSuccess {\n      customInputSubmissionId\n    }\n    ... on EnqueueCustomInputSubmissionValidationFailure {\n      message\n      fieldErrors {\n        field\n        message\n      }\n    }\n    ... on AuthenticationRequired {\n      message\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ExecutionUnavailable {\n      message\n    }\n    ... on ExecutionBusy {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "b0c491227fd52cd7536036736c732d7a";

export default node;

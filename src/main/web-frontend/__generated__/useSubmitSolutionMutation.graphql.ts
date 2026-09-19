/**
 * @generated SignedSource<<e04bed9ce69502dbec68ed7d357cf5bd>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type SubmitSolutionInput = {
  problemLanguageId: string;
  sourceCode: string;
};
export type useSubmitSolutionMutation$variables = {
  input: SubmitSolutionInput;
};
export type useSubmitSolutionMutation$data = {
  readonly submitSolution: {
    readonly __typename: "AuthenticationRequired";
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
    readonly __typename: "SubmissionValidationFailure";
    readonly fieldErrors: ReadonlyArray<{
      readonly message: string;
    }>;
    readonly message: string;
  } | {
    readonly __typename: "SubmitSolutionSuccess";
    readonly submission: {
      readonly id: string;
    };
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type useSubmitSolutionMutation = {
  response: useSubmitSolutionMutation$data;
  variables: useSubmitSolutionMutation$variables;
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
    "name": "submitSolution",
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
            "concreteType": "Submission",
            "kind": "LinkedField",
            "name": "submission",
            "plural": false,
            "selections": [
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "id",
                "storageKey": null
              }
            ],
            "storageKey": null
          }
        ],
        "type": "SubmitSolutionSuccess",
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
        "type": "SubmissionValidationFailure",
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
    "name": "useSubmitSolutionMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "useSubmitSolutionMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "c093a846659e4212a69c4535a55691a0",
    "id": null,
    "metadata": {},
    "name": "useSubmitSolutionMutation",
    "operationKind": "mutation",
    "text": "mutation useSubmitSolutionMutation(\n  $input: SubmitSolutionInput!\n) {\n  submitSolution(input: $input) {\n    __typename\n    ... on SubmitSolutionSuccess {\n      submission {\n        id\n      }\n    }\n    ... on SubmissionValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on AuthenticationRequired {\n      message\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ExecutionUnavailable {\n      message\n    }\n    ... on ExecutionBusy {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "fc92de5fffd8df59f70fd2d4664749b6";

export default node;

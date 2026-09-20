/**
 * @generated SignedSource<<d85e88650baa9b37794546cea1fdd514>>
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
    readonly __typename: "ProblemSubmissionValidationFailure";
    readonly fieldErrors: ReadonlyArray<{
      readonly message: string;
    }>;
    readonly message: string;
  } | {
    readonly __typename: "SubmitSolutionSuccess";
    readonly problemSubmission: {
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
            "concreteType": "ProblemSubmission",
            "kind": "LinkedField",
            "name": "problemSubmission",
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
        "type": "ProblemSubmissionValidationFailure",
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
    "cacheID": "3ace10ecaea63ef9aea60c59f8ba17df",
    "id": null,
    "metadata": {},
    "name": "useSubmitSolutionMutation",
    "operationKind": "mutation",
    "text": "mutation useSubmitSolutionMutation(\n  $input: SubmitSolutionInput!\n) {\n  submitSolution(input: $input) {\n    __typename\n    ... on SubmitSolutionSuccess {\n      problemSubmission {\n        id\n      }\n    }\n    ... on ProblemSubmissionValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on AuthenticationRequired {\n      message\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ExecutionUnavailable {\n      message\n    }\n    ... on ExecutionBusy {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "03f4677dc89a56aec8da74cc8a46200f";

export default node;

/**
 * @generated SignedSource<<459de8789eb889e5c21a0c7d62ea4db5>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type CreateProblemHiddenTestCaseInput = {
  expectedOutputJson: string;
  explanationMarkdown?: string | null | undefined;
  inputJson: string;
  problemId: string;
};
export type ProblemTestCaseCreationFormMutation$variables = {
  input: CreateProblemHiddenTestCaseInput;
};
export type ProblemTestCaseCreationFormMutation$data = {
  readonly createProblemHiddenTestCase: {
    readonly __typename: "CreateProblemHiddenTestCaseSuccess";
    readonly problem: {
      readonly id: string;
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
      readonly field: string;
      readonly message: string;
    }>;
    readonly message: string;
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type ProblemTestCaseCreationFormMutation = {
  response: ProblemTestCaseCreationFormMutation$data;
  variables: ProblemTestCaseCreationFormMutation$variables;
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
    "name": "createProblemHiddenTestCase",
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
        "type": "CreateProblemHiddenTestCaseSuccess",
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
    "name": "ProblemTestCaseCreationFormMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemTestCaseCreationFormMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "f155078169d7352e6cf392efcdfbfbfa",
    "id": null,
    "metadata": {},
    "name": "ProblemTestCaseCreationFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemTestCaseCreationFormMutation(\n  $input: CreateProblemHiddenTestCaseInput!\n) {\n  createProblemHiddenTestCase(input: $input) {\n    __typename\n    ... on CreateProblemHiddenTestCaseSuccess {\n      problem {\n        id\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        field\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "00e2c296acfd227066850690211998a6";

export default node;

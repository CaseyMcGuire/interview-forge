/**
 * @generated SignedSource<<90084f3353e97a73176e054176272d79>>
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
export type ProblemHiddenTestCaseCreationFormMutation$variables = {
  input: CreateProblemHiddenTestCaseInput;
};
export type ProblemHiddenTestCaseCreationFormMutation$data = {
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
export type ProblemHiddenTestCaseCreationFormMutation = {
  response: ProblemHiddenTestCaseCreationFormMutation$data;
  variables: ProblemHiddenTestCaseCreationFormMutation$variables;
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
    "name": "ProblemHiddenTestCaseCreationFormMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemHiddenTestCaseCreationFormMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "7e6b91819acf5477cb3db9ba1b6848ed",
    "id": null,
    "metadata": {},
    "name": "ProblemHiddenTestCaseCreationFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemHiddenTestCaseCreationFormMutation(\n  $input: CreateProblemHiddenTestCaseInput!\n) {\n  createProblemHiddenTestCase(input: $input) {\n    __typename\n    ... on CreateProblemHiddenTestCaseSuccess {\n      problem {\n        id\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        field\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "d6f383ac6e71acceacc5b230a676bf56";

export default node;

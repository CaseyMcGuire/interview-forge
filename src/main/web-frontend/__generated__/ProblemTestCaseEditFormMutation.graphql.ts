/**
 * @generated SignedSource<<13bdaecef7eb82e4737387157b6c3d50>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type UpdateProblemTestCaseInput = {
  expectedOutputJson: string;
  explanationMarkdown?: string | null | undefined;
  id: string;
  inputJson: string;
};
export type ProblemTestCaseEditFormMutation$variables = {
  input: UpdateProblemTestCaseInput;
};
export type ProblemTestCaseEditFormMutation$data = {
  readonly updateProblemTestCase: {
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
    readonly __typename: "UpdateProblemTestCaseSuccess";
    readonly testCase: {
      readonly expectedOutputJson: string;
      readonly explanationMarkdown: string | null | undefined;
      readonly id: string;
      readonly inputJson: string;
    };
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type ProblemTestCaseEditFormMutation = {
  response: ProblemTestCaseEditFormMutation$data;
  variables: ProblemTestCaseEditFormMutation$variables;
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
    "name": "updateProblemTestCase",
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
            "concreteType": "ProblemTestCase",
            "kind": "LinkedField",
            "name": "testCase",
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
        "type": "UpdateProblemTestCaseSuccess",
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
    "name": "ProblemTestCaseEditFormMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemTestCaseEditFormMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "4747c2b3af348be5c25ec50493e857be",
    "id": null,
    "metadata": {},
    "name": "ProblemTestCaseEditFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemTestCaseEditFormMutation(\n  $input: UpdateProblemTestCaseInput!\n) {\n  updateProblemTestCase(input: $input) {\n    __typename\n    ... on UpdateProblemTestCaseSuccess {\n      testCase {\n        id\n        inputJson\n        expectedOutputJson\n        explanationMarkdown\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        field\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "aa65009e10a520e3df35817ce132c785";

export default node;

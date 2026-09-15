/**
 * @generated SignedSource<<c63663030bc4fcd8509f12f4dbbb00df>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type UpdateProblemExampleInput = {
  expectedOutputJson?: string | null | undefined;
  explanationMarkdown?: string | null | undefined;
  id: string;
  inputJson?: string | null | undefined;
};
export type ProblemExampleEditFormMutation$variables = {
  input: UpdateProblemExampleInput;
};
export type ProblemExampleEditFormMutation$data = {
  readonly updateProblemExample: {
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
    readonly __typename: "UpdateProblemExampleSuccess";
    readonly example: {
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
export type ProblemExampleEditFormMutation = {
  response: ProblemExampleEditFormMutation$data;
  variables: ProblemExampleEditFormMutation$variables;
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
    "name": "updateProblemExample",
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
            "concreteType": "ProblemExample",
            "kind": "LinkedField",
            "name": "example",
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
        "type": "UpdateProblemExampleSuccess",
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
    "name": "ProblemExampleEditFormMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemExampleEditFormMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "011db2035652c3a7d0764bcdf9979265",
    "id": null,
    "metadata": {},
    "name": "ProblemExampleEditFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemExampleEditFormMutation(\n  $input: UpdateProblemExampleInput!\n) {\n  updateProblemExample(input: $input) {\n    __typename\n    ... on UpdateProblemExampleSuccess {\n      example {\n        id\n        inputJson\n        expectedOutputJson\n        explanationMarkdown\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        field\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "83702d6989c3c6f469ac36689f7afaff";

export default node;

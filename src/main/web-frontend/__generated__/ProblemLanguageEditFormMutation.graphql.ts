/**
 * @generated SignedSource<<2220ad07b77eee7ee380248f7f38dca6>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type UpdateProblemLanguageInput = {
  id: string;
  starterCode?: string | null | undefined;
};
export type ProblemLanguageEditFormMutation$variables = {
  input: UpdateProblemLanguageInput;
};
export type ProblemLanguageEditFormMutation$data = {
  readonly updateProblemLanguage: {
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
    readonly __typename: "UpdateProblemLanguageSuccess";
    readonly problemLanguage: {
      readonly id: string;
      readonly starterCode: string;
    };
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type ProblemLanguageEditFormMutation = {
  response: ProblemLanguageEditFormMutation$data;
  variables: ProblemLanguageEditFormMutation$variables;
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
    "name": "updateProblemLanguage",
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
                "name": "starterCode",
                "storageKey": null
              }
            ],
            "storageKey": null
          }
        ],
        "type": "UpdateProblemLanguageSuccess",
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
    "name": "ProblemLanguageEditFormMutation",
    "selections": (v3/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemLanguageEditFormMutation",
    "selections": (v3/*:: as any*/)
  },
  "params": {
    "cacheID": "4556ab643d711cb6e51ac4b86dfc6504",
    "id": null,
    "metadata": {},
    "name": "ProblemLanguageEditFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemLanguageEditFormMutation(\n  $input: UpdateProblemLanguageInput!\n) {\n  updateProblemLanguage(input: $input) {\n    __typename\n    ... on UpdateProblemLanguageSuccess {\n      problemLanguage {\n        id\n        starterCode\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "e202e8e7daa4bc1f9fae18a776ef322f";

export default node;

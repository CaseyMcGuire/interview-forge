/**
 * @generated SignedSource<<4df6624c1ee4b7d5e7a6f53c49bd39d2>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type CreateTagInput = {
  displayName: string;
  slug: string;
};
export type TagCreationDialogMutation$variables = {
  input: CreateTagInput;
};
export type TagCreationDialogMutation$data = {
  readonly createTag: {
    readonly __typename: "CreateTagSuccess";
    readonly tag: {
      readonly displayName: string;
      readonly id: string;
      readonly slug: string;
    };
  } | {
    readonly __typename: "TagForbidden";
    readonly message: string;
  } | {
    readonly __typename: "TagValidationFailure";
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
export type TagCreationDialogMutation = {
  response: TagCreationDialogMutation$data;
  variables: TagCreationDialogMutation$variables;
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
    "name": "createTag",
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
            "concreteType": "Tag",
            "kind": "LinkedField",
            "name": "tag",
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
                "name": "slug",
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "displayName",
                "storageKey": null
              }
            ],
            "storageKey": null
          }
        ],
        "type": "CreateTagSuccess",
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
        "type": "TagValidationFailure",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": [
          (v1/*:: as any*/)
        ],
        "type": "TagForbidden",
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
    "name": "TagCreationDialogMutation",
    "selections": (v2/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "TagCreationDialogMutation",
    "selections": (v2/*:: as any*/)
  },
  "params": {
    "cacheID": "4146f3f38ee8fd6c0b42ad32ec9669c7",
    "id": null,
    "metadata": {},
    "name": "TagCreationDialogMutation",
    "operationKind": "mutation",
    "text": "mutation TagCreationDialogMutation(\n  $input: CreateTagInput!\n) {\n  createTag(input: $input) {\n    __typename\n    ... on CreateTagSuccess {\n      tag {\n        id\n        slug\n        displayName\n      }\n    }\n    ... on TagValidationFailure {\n      message\n      fieldErrors {\n        field\n        message\n      }\n    }\n    ... on TagForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "45559b6fc9cb54c2f49fd51d4b1d5d3b";

export default node;

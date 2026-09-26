/**
 * @generated SignedSource<<8193c8fed41396863eaeaf59b1bab171>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type ProblemDifficulty = "EASY" | "HARD" | "MEDIUM" | "%future added value";
export type UpdateProblemInput = {
  difficulty?: ProblemDifficulty | null | undefined;
  id: string;
  statementMarkdown?: string | null | undefined;
  tagIds?: ReadonlyArray<string> | null | undefined;
  title?: string | null | undefined;
};
export type ProblemDetailsFormMutation$variables = {
  input: UpdateProblemInput;
};
export type ProblemDetailsFormMutation$data = {
  readonly updateProblem: {
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
    readonly __typename: "UpdateProblemSuccess";
    readonly problem: {
      readonly difficulty: ProblemDifficulty;
      readonly id: string;
      readonly statementMarkdown: string;
      readonly tags: ReadonlyArray<{
        readonly displayName: string;
        readonly id: string;
      }>;
      readonly title: string;
    };
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type ProblemDetailsFormMutation = {
  response: ProblemDetailsFormMutation$data;
  variables: ProblemDetailsFormMutation$variables;
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
  "name": "id",
  "storageKey": null
},
v2 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "message",
  "storageKey": null
},
v3 = [
  (v2/*:: as any*/)
],
v4 = [
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
    "name": "updateProblem",
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
              (v1/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "title",
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "statementMarkdown",
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "difficulty",
                "storageKey": null
              },
              {
                "alias": null,
                "args": null,
                "concreteType": "Tag",
                "kind": "LinkedField",
                "name": "tags",
                "plural": true,
                "selections": [
                  (v1/*:: as any*/),
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
            "storageKey": null
          }
        ],
        "type": "UpdateProblemSuccess",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": [
          (v2/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "concreteType": "FieldError",
            "kind": "LinkedField",
            "name": "fieldErrors",
            "plural": true,
            "selections": (v3/*:: as any*/),
            "storageKey": null
          }
        ],
        "type": "ProblemValidationFailure",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v3/*:: as any*/),
        "type": "ProblemNotFound",
        "abstractKey": null
      },
      {
        "kind": "InlineFragment",
        "selections": (v3/*:: as any*/),
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
    "name": "ProblemDetailsFormMutation",
    "selections": (v4/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemDetailsFormMutation",
    "selections": (v4/*:: as any*/)
  },
  "params": {
    "cacheID": "1bdb0d84f5c1fbdf76bbd72352873348",
    "id": null,
    "metadata": {},
    "name": "ProblemDetailsFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemDetailsFormMutation(\n  $input: UpdateProblemInput!\n) {\n  updateProblem(input: $input) {\n    __typename\n    ... on UpdateProblemSuccess {\n      problem {\n        id\n        title\n        statementMarkdown\n        difficulty\n        tags {\n          id\n          displayName\n        }\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "ac67998e6bc74dc8428dbacaf664294a";

export default node;

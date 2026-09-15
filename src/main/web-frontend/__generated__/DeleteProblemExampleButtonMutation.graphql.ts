/**
 * @generated SignedSource<<3388f77e1bc8cd90900b553889c89f6c>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type DeleteProblemExampleInput = {
  id: string;
};
export type DeleteProblemExampleButtonMutation$variables = {
  input: DeleteProblemExampleInput;
};
export type DeleteProblemExampleButtonMutation$data = {
  readonly deleteProblemExample: {
    readonly __typename: "DeleteProblemExampleSuccess";
    readonly deletedExampleId: string;
    readonly problem: {
      readonly examples: ReadonlyArray<{
        readonly id: string;
        readonly " $fragmentSpreads": FragmentRefs<"ProblemExampleEditForm_example">;
      }>;
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
      readonly message: string;
    }>;
    readonly message: string;
  } | {
    // This will never be '%other', but we need some
    // value in case none of the concrete values match.
    readonly __typename: "%other";
  };
};
export type DeleteProblemExampleButtonMutation = {
  response: DeleteProblemExampleButtonMutation$data;
  variables: DeleteProblemExampleButtonMutation$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "input"
  }
],
v1 = [
  {
    "kind": "Variable",
    "name": "input",
    "variableName": "input"
  }
],
v2 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "__typename",
  "storageKey": null
},
v3 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "deletedExampleId",
  "storageKey": null
},
v4 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
},
v5 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "message",
  "storageKey": null
},
v6 = [
  (v5/*:: as any*/)
],
v7 = {
  "kind": "InlineFragment",
  "selections": [
    (v5/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "concreteType": "FieldError",
      "kind": "LinkedField",
      "name": "fieldErrors",
      "plural": true,
      "selections": (v6/*:: as any*/),
      "storageKey": null
    }
  ],
  "type": "ProblemValidationFailure",
  "abstractKey": null
},
v8 = {
  "kind": "InlineFragment",
  "selections": (v6/*:: as any*/),
  "type": "ProblemNotFound",
  "abstractKey": null
},
v9 = {
  "kind": "InlineFragment",
  "selections": (v6/*:: as any*/),
  "type": "ProblemForbidden",
  "abstractKey": null
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": null,
    "name": "DeleteProblemExampleButtonMutation",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": null,
        "kind": "LinkedField",
        "name": "deleteProblemExample",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          {
            "kind": "InlineFragment",
            "selections": [
              (v3/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "concreteType": "Problem",
                "kind": "LinkedField",
                "name": "problem",
                "plural": false,
                "selections": [
                  (v4/*:: as any*/),
                  {
                    "alias": null,
                    "args": null,
                    "concreteType": "ProblemExample",
                    "kind": "LinkedField",
                    "name": "examples",
                    "plural": true,
                    "selections": [
                      (v4/*:: as any*/),
                      {
                        "args": null,
                        "kind": "FragmentSpread",
                        "name": "ProblemExampleEditForm_example"
                      }
                    ],
                    "storageKey": null
                  }
                ],
                "storageKey": null
              }
            ],
            "type": "DeleteProblemExampleSuccess",
            "abstractKey": null
          },
          (v7/*:: as any*/),
          (v8/*:: as any*/),
          (v9/*:: as any*/)
        ],
        "storageKey": null
      }
    ],
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "DeleteProblemExampleButtonMutation",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": null,
        "kind": "LinkedField",
        "name": "deleteProblemExample",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          {
            "kind": "InlineFragment",
            "selections": [
              (v3/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "filters": null,
                "handle": "deleteRecord",
                "key": "",
                "kind": "ScalarHandle",
                "name": "deletedExampleId"
              },
              {
                "alias": null,
                "args": null,
                "concreteType": "Problem",
                "kind": "LinkedField",
                "name": "problem",
                "plural": false,
                "selections": [
                  (v4/*:: as any*/),
                  {
                    "alias": null,
                    "args": null,
                    "concreteType": "ProblemExample",
                    "kind": "LinkedField",
                    "name": "examples",
                    "plural": true,
                    "selections": [
                      (v4/*:: as any*/),
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
                "storageKey": null
              }
            ],
            "type": "DeleteProblemExampleSuccess",
            "abstractKey": null
          },
          (v7/*:: as any*/),
          (v8/*:: as any*/),
          (v9/*:: as any*/)
        ],
        "storageKey": null
      }
    ]
  },
  "params": {
    "cacheID": "c8c6eb0dc95148189375631ca6532473",
    "id": null,
    "metadata": {},
    "name": "DeleteProblemExampleButtonMutation",
    "operationKind": "mutation",
    "text": "mutation DeleteProblemExampleButtonMutation(\n  $input: DeleteProblemExampleInput!\n) {\n  deleteProblemExample(input: $input) {\n    __typename\n    ... on DeleteProblemExampleSuccess {\n      deletedExampleId\n      problem {\n        id\n        examples {\n          id\n          ...ProblemExampleEditForm_example\n        }\n      }\n    }\n    ... on ProblemValidationFailure {\n      message\n      fieldErrors {\n        message\n      }\n    }\n    ... on ProblemNotFound {\n      message\n    }\n    ... on ProblemForbidden {\n      message\n    }\n  }\n}\n\nfragment ProblemExampleEditForm_example on ProblemExample {\n  id\n  inputJson\n  expectedOutputJson\n  explanationMarkdown\n}\n"
  }
};
})();

(node as any).hash = "a3ccb1255b727006873430e342167e3d";

export default node;

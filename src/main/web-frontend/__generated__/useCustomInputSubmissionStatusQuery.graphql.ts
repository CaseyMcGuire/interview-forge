/**
 * @generated SignedSource<<38c616a61557ce76485e242cbeeafd15>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type CustomInputSubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
export type useCustomInputSubmissionStatusQuery$variables = {
  id: string;
};
export type useCustomInputSubmissionStatusQuery$data = {
  readonly customInputSubmission: {
    readonly id: string;
    readonly status: CustomInputSubmissionStatus;
    readonly " $fragmentSpreads": FragmentRefs<"TestPanel_submission">;
  } | null | undefined;
};
export type useCustomInputSubmissionStatusQuery = {
  response: useCustomInputSubmissionStatusQuery$data;
  variables: useCustomInputSubmissionStatusQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "id"
  }
],
v1 = [
  {
    "kind": "Variable",
    "name": "id",
    "variableName": "id"
  }
],
v2 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
},
v3 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "status",
  "storageKey": null
},
v4 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "outcome",
  "storageKey": null
},
v5 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "publicErrorMessage",
  "storageKey": null
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "useCustomInputSubmissionStatusQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "CustomInputSubmission",
        "kind": "LinkedField",
        "name": "customInputSubmission",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          {
            "args": null,
            "kind": "FragmentSpread",
            "name": "TestPanel_submission"
          }
        ],
        "storageKey": null
      }
    ],
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "useCustomInputSubmissionStatusQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "CustomInputSubmission",
        "kind": "LinkedField",
        "name": "customInputSubmission",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          (v4/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "totalCases",
            "storageKey": null
          },
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "passedCases",
            "storageKey": null
          },
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "runtimeMs",
            "storageKey": null
          },
          (v5/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "concreteType": "CustomInputSubmissionCaseResult",
            "kind": "LinkedField",
            "name": "caseResults",
            "plural": true,
            "selections": [
              (v4/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "kind": "ScalarField",
                "name": "output",
                "storageKey": null
              },
              (v5/*:: as any*/),
              {
                "alias": null,
                "args": null,
                "concreteType": "CustomTestCase",
                "kind": "LinkedField",
                "name": "testCase",
                "plural": false,
                "selections": [
                  (v2/*:: as any*/),
                  {
                    "alias": null,
                    "args": null,
                    "kind": "ScalarField",
                    "name": "position",
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
                  }
                ],
                "storageKey": null
              }
            ],
            "storageKey": null
          }
        ],
        "storageKey": null
      }
    ]
  },
  "params": {
    "cacheID": "ad8422b86a0b66402456111bc34360c5",
    "id": null,
    "metadata": {},
    "name": "useCustomInputSubmissionStatusQuery",
    "operationKind": "query",
    "text": "query useCustomInputSubmissionStatusQuery(\n  $id: ID!\n) {\n  customInputSubmission(id: $id) {\n    id\n    status\n    ...TestPanel_submission\n  }\n}\n\nfragment TestPanel_submission on CustomInputSubmission {\n  status\n  outcome\n  totalCases\n  passedCases\n  runtimeMs\n  publicErrorMessage\n  caseResults {\n    outcome\n    output\n    publicErrorMessage\n    testCase {\n      id\n      position\n      inputJson\n      expectedOutputJson\n    }\n  }\n}\n"
  }
};
})();

(node as any).hash = "db1110ad514ee5599a40c4695b4dc9a0";

export default node;

/**
 * @generated SignedSource<<ab59538ef128d52b19f03821eeadb2db>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type SubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
export type useSubmissionStatusQuery$variables = {
  id: string;
};
export type useSubmissionStatusQuery$data = {
  readonly submission: {
    readonly id: string;
    readonly status: SubmissionStatus;
    readonly " $fragmentSpreads": FragmentRefs<"SubmissionResultPanel_submission">;
  } | null | undefined;
};
export type useSubmissionStatusQuery = {
  response: useSubmissionStatusQuery$data;
  variables: useSubmissionStatusQuery$variables;
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
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "useSubmissionStatusQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "Submission",
        "kind": "LinkedField",
        "name": "submission",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          {
            "args": null,
            "kind": "FragmentSpread",
            "name": "SubmissionResultPanel_submission"
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
    "name": "useSubmissionStatusQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "Submission",
        "kind": "LinkedField",
        "name": "submission",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "verdict",
            "storageKey": null
          },
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
          {
            "alias": null,
            "args": null,
            "kind": "ScalarField",
            "name": "publicErrorMessage",
            "storageKey": null
          },
          {
            "alias": null,
            "args": null,
            "concreteType": "SubmissionFailedExample",
            "kind": "LinkedField",
            "name": "failedExample",
            "plural": false,
            "selections": [
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
                "name": "output",
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
    "cacheID": "710c814fe40e3ce8c3f45290725a190f",
    "id": null,
    "metadata": {},
    "name": "useSubmissionStatusQuery",
    "operationKind": "query",
    "text": "query useSubmissionStatusQuery(\n  $id: ID!\n) {\n  submission(id: $id) {\n    id\n    status\n    ...SubmissionResultPanel_submission\n  }\n}\n\nfragment SubmissionFailedExample_example on SubmissionFailedExample {\n  inputJson\n  expectedOutputJson\n  output\n}\n\nfragment SubmissionResultPanel_submission on Submission {\n  status\n  verdict\n  totalCases\n  passedCases\n  runtimeMs\n  publicErrorMessage\n  failedExample {\n    ...SubmissionFailedExample_example\n  }\n}\n"
  }
};
})();

(node as any).hash = "1e8856effb888e06b0a68845d9c976a0";

export default node;

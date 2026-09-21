/**
 * @generated SignedSource<<06ae9e00bf30ce77cda6b56d8e1216f9>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemSubmissionStatus = "FINISHED" | "QUEUED" | "RUNNING" | "%future added value";
export type ProblemSubmissionVerdict = "ACCEPTED" | "COMPILE_ERROR" | "INTERNAL_ERROR" | "MEMORY_LIMIT_EXCEEDED" | "PENDING" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
export type useProblemSubmissionStatusQuery$variables = {
  id: string;
};
export type useProblemSubmissionStatusQuery$data = {
  readonly problemSubmission: {
    readonly id: string;
    readonly status: ProblemSubmissionStatus;
    readonly verdict: ProblemSubmissionVerdict;
    readonly " $fragmentSpreads": FragmentRefs<"ProblemSubmissionResultPanel_problemSubmission">;
  } | null | undefined;
};
export type useProblemSubmissionStatusQuery = {
  response: useProblemSubmissionStatusQuery$data;
  variables: useProblemSubmissionStatusQuery$variables;
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
  "name": "verdict",
  "storageKey": null
};
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "useProblemSubmissionStatusQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "ProblemSubmission",
        "kind": "LinkedField",
        "name": "problemSubmission",
        "plural": false,
        "selections": [
          (v2/*:: as any*/),
          (v3/*:: as any*/),
          (v4/*:: as any*/),
          {
            "args": null,
            "kind": "FragmentSpread",
            "name": "ProblemSubmissionResultPanel_problemSubmission"
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
    "name": "useProblemSubmissionStatusQuery",
    "selections": [
      {
        "alias": null,
        "args": (v1/*:: as any*/),
        "concreteType": "ProblemSubmission",
        "kind": "LinkedField",
        "name": "problemSubmission",
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
            "concreteType": "ProblemSubmissionFailedExample",
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
    "cacheID": "e550c2b0b7802093ea2a88a97319bca9",
    "id": null,
    "metadata": {},
    "name": "useProblemSubmissionStatusQuery",
    "operationKind": "query",
    "text": "query useProblemSubmissionStatusQuery(\n  $id: ID!\n) {\n  problemSubmission(id: $id) {\n    id\n    status\n    verdict\n    ...ProblemSubmissionResultPanel_problemSubmission\n  }\n}\n\nfragment ProblemSubmissionFailedExample_example on ProblemSubmissionFailedExample {\n  inputJson\n  expectedOutputJson\n  output\n}\n\nfragment ProblemSubmissionResultPanel_problemSubmission on ProblemSubmission {\n  status\n  verdict\n  totalCases\n  passedCases\n  runtimeMs\n  publicErrorMessage\n  failedExample {\n    ...ProblemSubmissionFailedExample_example\n  }\n}\n"
  }
};
})();

(node as any).hash = "5d8e721ce105464bd1c2e60bc2f2c0a5";

export default node;

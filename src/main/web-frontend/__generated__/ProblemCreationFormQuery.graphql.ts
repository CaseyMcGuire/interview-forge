/**
 * @generated SignedSource<<5d59e3842bc5d9a26e6ca3edfad9ce4d>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type ProblemCreationFormQuery$variables = Record<PropertyKey, never>;
export type ProblemCreationFormQuery$data = {
  readonly languages: ReadonlyArray<{
    readonly displayName: string;
    readonly id: string;
    readonly key: string;
  }>;
};
export type ProblemCreationFormQuery = {
  response: ProblemCreationFormQuery$data;
  variables: ProblemCreationFormQuery$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "alias": null,
    "args": null,
    "concreteType": "Language",
    "kind": "LinkedField",
    "name": "languages",
    "plural": true,
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
        "name": "key",
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
];
return {
  "fragment": {
    "argumentDefinitions": [],
    "kind": "Fragment",
    "metadata": {
      "throwOnFieldError": true
    },
    "name": "ProblemCreationFormQuery",
    "selections": (v0/*:: as any*/),
    "type": "Query",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": [],
    "kind": "Operation",
    "name": "ProblemCreationFormQuery",
    "selections": (v0/*:: as any*/)
  },
  "params": {
    "cacheID": "dfd3511e95edf3b1cbc0416f5282b42c",
    "id": null,
    "metadata": {},
    "name": "ProblemCreationFormQuery",
    "operationKind": "query",
    "text": "query ProblemCreationFormQuery {\n  languages {\n    id\n    key\n    displayName\n  }\n}\n"
  }
};
})();

(node as any).hash = "a9cad168d95298f4c07c6752073dadda";

export default node;

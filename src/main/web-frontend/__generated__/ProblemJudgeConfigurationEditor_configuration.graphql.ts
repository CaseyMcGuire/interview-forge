/**
 * @generated SignedSource<<f8794875e0ea3901d1e8a335292fb33e>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemJudgeConfigurationEditor_configuration$data = {
  readonly id: string;
  readonly judgeConfiguration: {
    readonly checkerSource: string | null | undefined;
    readonly id: string;
    readonly memoryLimitMb: number;
    readonly runtime: string;
    readonly testDriverCode: string;
    readonly timeLimitMs: number;
  } | null | undefined;
  readonly language: {
    readonly displayName: string;
    readonly key: string;
  };
  readonly " $fragmentType": "ProblemJudgeConfigurationEditor_configuration";
};
export type ProblemJudgeConfigurationEditor_configuration$key = {
  readonly " $data"?: ProblemJudgeConfigurationEditor_configuration$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemJudgeConfigurationEditor_configuration">;
};

const node: ReaderFragment = (function(){
var v0 = {
  "alias": null,
  "args": null,
  "kind": "ScalarField",
  "name": "id",
  "storageKey": null
};
return {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemJudgeConfigurationEditor_configuration",
  "selections": [
    (v0/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "concreteType": "Language",
      "kind": "LinkedField",
      "name": "language",
      "plural": false,
      "selections": [
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
    },
    {
      "alias": null,
      "args": null,
      "concreteType": "JudgeConfiguration",
      "kind": "LinkedField",
      "name": "judgeConfiguration",
      "plural": false,
      "selections": [
        (v0/*:: as any*/),
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "runtime",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "testDriverCode",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "checkerSource",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "timeLimitMs",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "memoryLimitMb",
          "storageKey": null
        }
      ],
      "storageKey": null
    }
  ],
  "type": "ProblemLanguage",
  "abstractKey": null
};
})();

(node as any).hash = "2102f5742256b318518150915ea1e5d3";

export default node;

/**
 * @generated SignedSource<<863b3818ddcbbf88425a4a2b4c53b81b>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type TestDriverType = "JSON_TEST_DRIVER" | "MAIN_FUNCTION" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type ProblemJudgeConfigurationEditor_configuration$data = {
  readonly id: string;
  readonly judgeConfiguration: {
    readonly id: string;
    readonly memoryLimitMb: number;
    readonly testDriverCode: string;
    readonly testDriverType: TestDriverType;
    readonly timeLimitMs: number;
  } | null | undefined;
  readonly language: {
    readonly displayName: string;
    readonly key: string;
    readonly testDriverTypes: ReadonlyArray<{
      readonly type: TestDriverType;
    }>;
    readonly " $fragmentSpreads": FragmentRefs<"ProblemTestDriverField_language">;
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
        },
        {
          "alias": null,
          "args": null,
          "concreteType": "TestDriverTypeInfo",
          "kind": "LinkedField",
          "name": "testDriverTypes",
          "plural": true,
          "selections": [
            {
              "alias": null,
              "args": null,
              "kind": "ScalarField",
              "name": "type",
              "storageKey": null
            }
          ],
          "storageKey": null
        },
        {
          "args": null,
          "kind": "FragmentSpread",
          "name": "ProblemTestDriverField_language"
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
          "name": "testDriverType",
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

(node as any).hash = "bdbb696b2977090e421c8a878df782f5";

export default node;

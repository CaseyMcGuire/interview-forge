/**
 * @generated SignedSource<<b07d278ed4f5b27246774ae411a9c0f2>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemLanguageEditForm_configuration$data = {
  readonly id: string;
  readonly language: {
    readonly displayName: string;
    readonly key: string;
  };
  readonly starterCode: string;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemJudgeConfigurationEditor_configuration">;
  readonly " $fragmentType": "ProblemLanguageEditForm_configuration";
};
export type ProblemLanguageEditForm_configuration$key = {
  readonly " $data"?: ProblemLanguageEditForm_configuration$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemLanguageEditForm_configuration">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemLanguageEditForm_configuration",
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
    },
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
      "args": null,
      "kind": "FragmentSpread",
      "name": "ProblemJudgeConfigurationEditor_configuration"
    }
  ],
  "type": "ProblemLanguage",
  "abstractKey": null
};

(node as any).hash = "8f88e0733abe2135810812698052d306";

export default node;

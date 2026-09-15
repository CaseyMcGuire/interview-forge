/**
 * @generated SignedSource<<8f3718eec4ad6442d3d5732f82976352>>
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
    }
  ],
  "type": "ProblemLanguage",
  "abstractKey": null
};

(node as any).hash = "92b7120cd92503ffb5317c1b75b44af3";

export default node;

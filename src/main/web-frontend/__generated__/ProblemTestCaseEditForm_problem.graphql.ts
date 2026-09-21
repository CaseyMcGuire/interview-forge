/**
 * @generated SignedSource<<73b5aec106dee04e9e938afa1b354ab2>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemTestCaseEditForm_problem$data = {
  readonly languageConfigurations: ReadonlyArray<{
    readonly id: string;
    readonly language: {
      readonly displayName: string;
    };
  }>;
  readonly " $fragmentType": "ProblemTestCaseEditForm_problem";
};
export type ProblemTestCaseEditForm_problem$key = {
  readonly " $data"?: ProblemTestCaseEditForm_problem$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTestCaseEditForm_problem">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemTestCaseEditForm_problem",
  "selections": [
    {
      "alias": null,
      "args": null,
      "concreteType": "ProblemLanguage",
      "kind": "LinkedField",
      "name": "languageConfigurations",
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
          "concreteType": "Language",
          "kind": "LinkedField",
          "name": "language",
          "plural": false,
          "selections": [
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
  "type": "Problem",
  "abstractKey": null
};

(node as any).hash = "1daa0340a7c90de74eddc0ff51c738d8";

export default node;

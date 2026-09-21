/**
 * @generated SignedSource<<a0ede6f6eef44f94947aea21bb39f2db>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemTestCaseCreationForm_problem$data = {
  readonly id: string;
  readonly languageConfigurations: ReadonlyArray<{
    readonly id: string;
    readonly language: {
      readonly displayName: string;
    };
  }>;
  readonly " $fragmentType": "ProblemTestCaseCreationForm_problem";
};
export type ProblemTestCaseCreationForm_problem$key = {
  readonly " $data"?: ProblemTestCaseCreationForm_problem$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemTestCaseCreationForm_problem">;
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
  "name": "ProblemTestCaseCreationForm_problem",
  "selections": [
    (v0/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "concreteType": "ProblemLanguage",
      "kind": "LinkedField",
      "name": "languageConfigurations",
      "plural": true,
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
})();

(node as any).hash = "8fb703f092925243e33728d0e15d018d";

export default node;

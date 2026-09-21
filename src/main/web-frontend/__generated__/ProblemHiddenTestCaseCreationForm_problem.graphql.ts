/**
 * @generated SignedSource<<e1ac277fe7497e1dc34e454548d3edbb>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
import { FragmentRefs } from "relay-runtime";
export type ProblemHiddenTestCaseCreationForm_problem$data = {
  readonly id: string;
  readonly languageConfigurations: ReadonlyArray<{
    readonly id: string;
    readonly language: {
      readonly displayName: string;
    };
  }>;
  readonly " $fragmentType": "ProblemHiddenTestCaseCreationForm_problem";
};
export type ProblemHiddenTestCaseCreationForm_problem$key = {
  readonly " $data"?: ProblemHiddenTestCaseCreationForm_problem$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemHiddenTestCaseCreationForm_problem">;
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
  "name": "ProblemHiddenTestCaseCreationForm_problem",
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

(node as any).hash = "838b9f981d765b343bed51217b9102f2";

export default node;

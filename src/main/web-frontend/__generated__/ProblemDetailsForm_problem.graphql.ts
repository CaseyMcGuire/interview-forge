/**
 * @generated SignedSource<<52e0b189d42c74da642635b27f617b2d>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type ProblemDifficulty = "EASY" | "HARD" | "MEDIUM" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type ProblemDetailsForm_problem$data = {
  readonly difficulty: ProblemDifficulty;
  readonly id: string;
  readonly statementMarkdown: string;
  readonly tags: ReadonlyArray<{
    readonly id: string;
  }>;
  readonly title: string;
  readonly " $fragmentType": "ProblemDetailsForm_problem";
};
export type ProblemDetailsForm_problem$key = {
  readonly " $data"?: ProblemDetailsForm_problem$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemDetailsForm_problem">;
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
  "name": "ProblemDetailsForm_problem",
  "selections": [
    (v0/*:: as any*/),
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "title",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "statementMarkdown",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "difficulty",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "concreteType": "Tag",
      "kind": "LinkedField",
      "name": "tags",
      "plural": true,
      "selections": [
        (v0/*:: as any*/)
      ],
      "storageKey": null
    }
  ],
  "type": "Problem",
  "abstractKey": null
};
})();

(node as any).hash = "1c1cce9e007bf3b97daf29a9550c4c3f";

export default node;

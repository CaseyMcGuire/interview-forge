/**
 * @generated SignedSource<<86bbd059ce1c4c08353b7b8edec59fa2>>
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
  readonly title: string;
  readonly " $fragmentType": "ProblemDetailsForm_problem";
};
export type ProblemDetailsForm_problem$key = {
  readonly " $data"?: ProblemDetailsForm_problem$data;
  readonly " $fragmentSpreads": FragmentRefs<"ProblemDetailsForm_problem">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "ProblemDetailsForm_problem",
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
    }
  ],
  "type": "Problem",
  "abstractKey": null
};

(node as any).hash = "179941dbe8c8b5471b11e0a411ca5b31";

export default node;

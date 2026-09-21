/**
 * @generated SignedSource<<873001c831613e0583327c3e8ca3514e>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ReaderFragment } from 'relay-runtime';
export type CustomInputSubmissionCaseOutcome = "INTERNAL_ERROR" | "INVALID_OUTPUT" | "MEMORY_LIMIT_EXCEEDED" | "NOT_RUN" | "OUTPUT_LIMIT_EXCEEDED" | "PASSED" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "WRONG_ANSWER" | "%future added value";
import { FragmentRefs } from "relay-runtime";
export type CustomInputSubmissionCaseResult_result$data = {
  readonly outcome: CustomInputSubmissionCaseOutcome;
  readonly output: string;
  readonly publicErrorMessage: string | null | undefined;
  readonly testCase: {
    readonly expectedOutputJson: string | null | undefined;
    readonly inputJson: string;
    readonly position: number;
  };
  readonly " $fragmentType": "CustomInputSubmissionCaseResult_result";
};
export type CustomInputSubmissionCaseResult_result$key = {
  readonly " $data"?: CustomInputSubmissionCaseResult_result$data;
  readonly " $fragmentSpreads": FragmentRefs<"CustomInputSubmissionCaseResult_result">;
};

const node: ReaderFragment = {
  "argumentDefinitions": [],
  "kind": "Fragment",
  "metadata": null,
  "name": "CustomInputSubmissionCaseResult_result",
  "selections": [
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "outcome",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "output",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "kind": "ScalarField",
      "name": "publicErrorMessage",
      "storageKey": null
    },
    {
      "alias": null,
      "args": null,
      "concreteType": "CustomTestCase",
      "kind": "LinkedField",
      "name": "testCase",
      "plural": false,
      "selections": [
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "position",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "inputJson",
          "storageKey": null
        },
        {
          "alias": null,
          "args": null,
          "kind": "ScalarField",
          "name": "expectedOutputJson",
          "storageKey": null
        }
      ],
      "storageKey": null
    }
  ],
  "type": "CustomInputSubmissionCaseResult",
  "abstractKey": null
};

(node as any).hash = "2ac661b4a1c352d9488babd0bded12b9";

export default node;

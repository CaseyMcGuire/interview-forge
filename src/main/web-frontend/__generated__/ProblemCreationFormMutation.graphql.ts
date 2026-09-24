/**
 * @generated SignedSource<<6588e1b6bac76c52692daf8f125f0ba7>>
 * @lightSyntaxTransform
 */

/* tslint:disable */
/* eslint-disable */
// @ts-nocheck

import { ConcreteRequest } from 'relay-runtime';
export type ProblemDifficulty = "EASY" | "HARD" | "MEDIUM" | "%future added value";
export type CreateProblemInput = {
  difficulty: ProblemDifficulty;
  examples: ReadonlyArray<CreateProblemExampleInput>;
  languageConfigurations: ReadonlyArray<CreateProblemLanguageInput>;
  slug: string;
  statementMarkdown: string;
  tagIds?: ReadonlyArray<string>;
  title: string;
};
export type CreateProblemLanguageInput = {
  languageKey: string;
  starterCode: string;
};
export type CreateProblemExampleInput = {
  expectedOutputJson: string;
  explanationMarkdown?: string | null | undefined;
  inputJson: string;
};
export type ProblemCreationFormMutation$variables = {
  input: CreateProblemInput;
};
export type ProblemCreationFormMutation$data = {
  readonly createProblem: {
    readonly id: string;
    readonly slug: string;
  } | null | undefined;
};
export type ProblemCreationFormMutation = {
  response: ProblemCreationFormMutation$data;
  variables: ProblemCreationFormMutation$variables;
};

const node: ConcreteRequest = (function(){
var v0 = [
  {
    "defaultValue": null,
    "kind": "LocalArgument",
    "name": "input"
  }
],
v1 = [
  {
    "alias": null,
    "args": [
      {
        "kind": "Variable",
        "name": "input",
        "variableName": "input"
      }
    ],
    "concreteType": "Problem",
    "kind": "LinkedField",
    "name": "createProblem",
    "plural": false,
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
        "name": "slug",
        "storageKey": null
      }
    ],
    "storageKey": null
  }
];
return {
  "fragment": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Fragment",
    "metadata": null,
    "name": "ProblemCreationFormMutation",
    "selections": (v1/*:: as any*/),
    "type": "Mutation",
    "abstractKey": null
  },
  "kind": "Request",
  "operation": {
    "argumentDefinitions": (v0/*:: as any*/),
    "kind": "Operation",
    "name": "ProblemCreationFormMutation",
    "selections": (v1/*:: as any*/)
  },
  "params": {
    "cacheID": "4fde5fe00df3d163a1f233954a6be94b",
    "id": null,
    "metadata": {},
    "name": "ProblemCreationFormMutation",
    "operationKind": "mutation",
    "text": "mutation ProblemCreationFormMutation(\n  $input: CreateProblemInput!\n) {\n  createProblem(input: $input) {\n    id\n    slug\n  }\n}\n"
  }
};
})();

(node as any).hash = "24912e9d86ae4b4c3f1583870a7e0138";

export default node;

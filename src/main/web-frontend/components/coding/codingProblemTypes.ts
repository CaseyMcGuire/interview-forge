import type {ProblemPageQuery$data} from "__generated__/ProblemPageQuery.graphql";

export type CodingProblem = NonNullable<ProblemPageQuery$data["problem"]>;
export type ExampleTestCase = CodingProblem["examples"][number];

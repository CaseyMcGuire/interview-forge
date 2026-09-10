// THIS FILE IS GENERATED. DO NOT EDIT BY HAND.
// Run './gradlew generateClientRoutes' to regenerate.

type SpaRouteIds = { applicationId: string; routeId: string };

function routeWithoutParams(path: string, ids: SpaRouteIds) {
  return Object.assign(() => path, { path, ...ids });
}

export const GraphiqlRoutes = {
  Graphiql: routeWithoutParams("/graphiql", { applicationId: "graphiql", routeId: "Graphiql" }),
} as const;

export type GraphiqlRoute = keyof typeof GraphiqlRoutes;

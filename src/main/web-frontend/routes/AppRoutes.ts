// THIS FILE IS GENERATED. DO NOT EDIT BY HAND.
// Run './gradlew generateClientRoutes' to regenerate.

type SpaRouteIds = { applicationId: string; routeId: string };

function routeWithoutParams(path: string, ids: SpaRouteIds) {
  return Object.assign(() => path, { path, ...ids });
}

type RouteParamValue = string | number;

function encodeRouteParam(value: RouteParamValue): string {
  return encodeURIComponent(String(value));
}

function route<TParams extends object>(path: string, buildPath: (params: TParams) => string, ids: SpaRouteIds) {
  return Object.assign(buildPath, { path, ...ids });
}

export const AppRoutes = {
  About: routeWithoutParams("/about", { applicationId: "app", routeId: "About" }),
  Blog: routeWithoutParams("/blog", { applicationId: "app", routeId: "Blog" }),
  CreateProblem: routeWithoutParams("/problem/create", { applicationId: "app", routeId: "CreateProblem" }),
  CreateProblemTestCase: route(
    "/problem/:slug/test/create",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/test/create`,
    { applicationId: "app", routeId: "CreateProblemTestCase" }
  ),
  EditProblem: route(
    "/problem/:slug/edit",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/edit`,
    { applicationId: "app", routeId: "EditProblem" }
  ),
  EditProblemTestCase: route(
    "/problem/:slug/test/:id/edit",
    (params: { slug: string; id: string }) => `/problem/${encodeRouteParam(params.slug)}/test/${encodeRouteParam(params.id)}/edit`,
    { applicationId: "app", routeId: "EditProblemTestCase" }
  ),
  Home: routeWithoutParams("/", { applicationId: "app", routeId: "Home" }),
  Login: routeWithoutParams("/login", { applicationId: "app", routeId: "Login" }),
  Problem: route(
    "/problem/:slug",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}`,
    { applicationId: "app", routeId: "Problem" }
  ),
  ProblemTestCases: route(
    "/problem/:slug/tests",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/tests`,
    { applicationId: "app", routeId: "ProblemTestCases" }
  ),
  Problems: routeWithoutParams("/problems", { applicationId: "app", routeId: "Problems" }),
  Register: routeWithoutParams("/register", { applicationId: "app", routeId: "Register" }),
} as const;

export type AppRoute = keyof typeof AppRoutes;

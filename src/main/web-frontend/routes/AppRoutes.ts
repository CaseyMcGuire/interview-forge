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
  CreateProblemHiddenTestCase: route(
    "/problem/:slug/tests/create",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/tests/create`,
    { applicationId: "app", routeId: "CreateProblemHiddenTestCase" }
  ),
  EditProblem: route(
    "/problem/:slug/edit",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/edit`,
    { applicationId: "app", routeId: "EditProblem" }
  ),
  Home: routeWithoutParams("/", { applicationId: "app", routeId: "Home" }),
  Login: routeWithoutParams("/login", { applicationId: "app", routeId: "Login" }),
  Problem: route(
    "/problem/:slug",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}`,
    { applicationId: "app", routeId: "Problem" }
  ),
  Problems: routeWithoutParams("/problems", { applicationId: "app", routeId: "Problems" }),
  Register: routeWithoutParams("/register", { applicationId: "app", routeId: "Register" }),
} as const;

export type AppRoute = keyof typeof AppRoutes;

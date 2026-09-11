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
  Home: routeWithoutParams("/", { applicationId: "app", routeId: "Home" }),
  Login: routeWithoutParams("/login", { applicationId: "app", routeId: "Login" }),
  Problem: route(
    "/problem/:slug",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}`,
    { applicationId: "app", routeId: "Problem" }
  ),
  Register: routeWithoutParams("/register", { applicationId: "app", routeId: "Register" }),
} as const;

export type AppRoute = keyof typeof AppRoutes;

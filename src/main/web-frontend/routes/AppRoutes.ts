// THIS FILE IS GENERATED. DO NOT EDIT BY HAND.
// Run './gradlew generateClientRoutes' to regenerate.

type SpaRouteIds = { applicationId: string; routeId: string };

function routeWithoutParams(path: string, ids: SpaRouteIds) {
  return Object.assign(() => path, { path, ...ids });
}

export const AppRoutes = {
  About: routeWithoutParams("/about", { applicationId: "app", routeId: "About" }),
  Blog: routeWithoutParams("/blog", { applicationId: "app", routeId: "Blog" }),
  Home: routeWithoutParams("/", { applicationId: "app", routeId: "Home" }),
  Login: routeWithoutParams("/login", { applicationId: "app", routeId: "Login" }),
  Register: routeWithoutParams("/register", { applicationId: "app", routeId: "Register" }),
} as const;

export type AppRoute = keyof typeof AppRoutes;

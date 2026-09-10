import {
  createBrowserRouter, RouterProvider, type RouteObject,
} from "react-router";
import HomePage from "pages/HomePage";
import {createRelayEnvironment, RelayRoot} from "@spa-kit/react-relay";
import {renderComponent} from "@spa-kit/react";
import {spaRoutingResolver, withRouteAuthorization} from "@spa-kit/react-router";
import AboutPage from "./pages/AboutPage";
import BlogPage from "./pages/BlogPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CsrfUtils from "./utils/CsrfUtils";
import {AppRoutes} from "routes/AppRoutes";

// One route per generated AppRoutes entry (the single source of truth is
// spa-route-definitions/…/AppSpaApplication.kt). react-router's route.id comes straight
// from the generated routeId, which spaRoutingResolver sends to /__spa/route-decision;
// withRouteAuthorization gates each leaf on that decision before it renders. This app
// declares no server-side route rules, so every decision allows — the wiring exists so
// adding a rule (e.g. require login) needs no client changes.
const routes: RouteObject[] = [
  {
    id: AppRoutes.Home.routeId,
    path: AppRoutes.Home.path,
    element: <HomePage />
  },
  {
    id: AppRoutes.Login.routeId,
    path: AppRoutes.Login.path,
    element: <LoginPage />
  },
  {
    id: AppRoutes.Register.routeId,
    path: AppRoutes.Register.path,
    element: <RegisterPage />
  },
  {
    id: AppRoutes.About.routeId,
    path: AppRoutes.About.path,
    element: <AboutPage />
  },
  {
    id: AppRoutes.Blog.routeId,
    path: AppRoutes.Blog.path,
    element: <BlogPage />
  }
]

const router = createBrowserRouter(
  withRouteAuthorization(
    routes,
    spaRoutingResolver({
      applicationId: AppRoutes.Home.applicationId,
      // No route rules exist, and data is gated server-side regardless — if the
      // decision request itself fails, let navigation proceed
      onError: { type: "allow" },
    }),
  ),
)

const environment = createRelayEnvironment({
  headers: () => ({ [CsrfUtils.getHeader()]: CsrfUtils.getToken() }),
});

export function App() {
  return (
    <RelayRoot environment={environment} fallback={null}>
      <RouterProvider router={router} />
    </RelayRoot>
  );
}

renderComponent(<App />)

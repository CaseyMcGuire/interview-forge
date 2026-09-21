import {RouterProvider, type RouteObject} from "react-router";
import HomePage from "pages/HomePage";
import ProblemPage from "pages/ProblemPage";
import ProblemsPage from "pages/ProblemsPage";
import CreateProblemPage from "pages/CreateProblemPage";
import CreateProblemHiddenTestCasePage, {CreateProblemHiddenTestCasePageError} from "pages/CreateProblemHiddenTestCasePage";
import EditProblemPage, {EditProblemPageError} from "pages/EditProblemPage";
import {createRelayEnvironment, RelayRoot} from "@spa-kit/react-relay";
import {renderComponent} from "@spa-kit/react";
import {createSpaRoutingBrowserRouter} from "@spa-kit/react-router";
import AboutPage from "./pages/AboutPage";
import BlogPage from "./pages/BlogPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CsrfUtils from "./utils/CsrfUtils";
import {AppRoutes} from "routes/AppRoutes";

// One route per generated AppRoutes entry (the single source of truth is
// spa-route-definitions/…/AppSpaApplication.kt). The router sends the generated routeId
// to /__spa/route-decision and gates each page on that decision before it renders.
const routes: RouteObject[] = [
  {
    id: AppRoutes.Home.routeId,
    path: AppRoutes.Home.path,
    element: <HomePage />
  },
  {
    id: AppRoutes.Problems.routeId,
    path: AppRoutes.Problems.path,
    element: <ProblemsPage />
  },
  {
    id: AppRoutes.CreateProblem.routeId,
    path: AppRoutes.CreateProblem.path,
    element: <CreateProblemPage />
  },
  {
    id: AppRoutes.EditProblem.routeId,
    path: AppRoutes.EditProblem.path,
    element: <EditProblemPage />,
    errorElement: <EditProblemPageError />
  },
  {
    id: AppRoutes.CreateProblemHiddenTestCase.routeId,
    path: AppRoutes.CreateProblemHiddenTestCase.path,
    element: <CreateProblemHiddenTestCasePage />,
    errorElement: <CreateProblemHiddenTestCasePageError />
  },
  {
    id: AppRoutes.Problem.routeId,
    path: AppRoutes.Problem.path,
    element: <ProblemPage />
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

const router = createSpaRoutingBrowserRouter(routes, {
  applicationId: AppRoutes.Home.applicationId,
  // Use the server error page so a failed check cannot loop through another SPA route.
  onError: {type: "redirect", location: "/error"},
});

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

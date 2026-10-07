import {RouterProvider} from "react-router";
import HomePage from "pages/HomePage";
import ProblemPage from "pages/ProblemPage";
import ProblemsPage from "pages/ProblemsPage";
import CreateProblemPage from "pages/CreateProblemPage";
import CreateProblemTestCasePage, {CreateProblemTestCasePageError} from "pages/CreateProblemTestCasePage";
import ProblemTestCasesPage, {ProblemTestCasesPageError} from "pages/ProblemTestCasesPage";
import EditProblemTestCasePage, {EditProblemTestCasePageError} from "pages/EditProblemTestCasePage";
import EditProblemPage, {EditProblemPageError} from "pages/EditProblemPage";
import {createRelayEnvironment, RelayRoot} from "@spa-kit/react-relay";
import {renderComponent} from "@spa-kit/react";
import {createSpaRouter, createSpaRouteDecisionMiddleware} from "@spa-kit/react-router";
import AboutPage from "./pages/AboutPage";
import BlogPage from "./pages/BlogPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CsrfUtils from "./utils/CsrfUtils";
import {AppRoutes} from "routes/AppRoutes";

// Generated routes supply paths, parsers, and access-handler metadata. Authoring
// routes check /__spa/route-decision before rendering; public routes need no check.
const router = createSpaRouter(AppRoutes, {
  Home: {
    render: () => <HomePage />
  },
  Problems: {
    render: () => <ProblemsPage />
  },
  CreateProblem: {
    render: () => <CreateProblemPage />
  },
  EditProblem: {
    render: () => <EditProblemPage />,
    errorElement: <EditProblemPageError />
  },
  CreateProblemTestCase: {
    render: () => <CreateProblemTestCasePage />,
    errorElement: <CreateProblemTestCasePageError />
  },
  ProblemTestCases: {
    render: () => <ProblemTestCasesPage />,
    errorElement: <ProblemTestCasesPageError />
  },
  EditProblemTestCase: {
    render: () => <EditProblemTestCasePage />,
    errorElement: <EditProblemTestCasePageError />
  },
  Problem: {
    render: () => <ProblemPage />
  },
  Login: {
    render: () => <LoginPage />
  },
  Register: {
    render: () => <RegisterPage />
  },
  About: {
    render: () => <AboutPage />
  },
  Blog: {
    render: () => <BlogPage />
  }
}, {
  sharedMiddleware: [
    createSpaRouteDecisionMiddleware({
      // Use the server error page so a failed check cannot loop through another SPA route.
      onError: {type: "denied", destination: "/error"},
    }),
  ],
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

import {RouterProvider} from "react-router";
import {loadQuery} from "react-relay";
import HomePage from "pages/HomePage";
import ProblemPage, {problemPageQuery} from "pages/ProblemPage";
import ProblemsPage, {problemsPageQuery} from "pages/ProblemsPage";
import CreateProblemPage, {createProblemPageQuery} from "pages/CreateProblemPage";
import CreateProblemTestCasePage, {
  CreateProblemTestCasePageError,
  createProblemTestCasePageQuery,
} from "pages/CreateProblemTestCasePage";
import ProblemTestCasesPage, {ProblemTestCasesPageError, problemTestCasesPageQuery} from "pages/ProblemTestCasesPage";
import EditProblemTestCasePage, {
  EditProblemTestCasePageError,
  editProblemTestCasePageQuery,
} from "pages/EditProblemTestCasePage";
import EditProblemPage, {EditProblemPageError, editProblemPageQuery} from "pages/EditProblemPage";
import {createRelayEnvironment, RelayRoot} from "@spa-kit/react-relay";
import {renderComponent} from "@spa-kit/react";
import {createSpaRouter, createSpaRouteDecisionMiddleware} from "@spa-kit/react-router";
import AboutPage from "./pages/AboutPage";
import BlogPage from "./pages/BlogPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CsrfUtils from "./utils/CsrfUtils";
import {
  AppRoutes,
  type CreateProblemTestCaseContext,
  type EditProblemContext,
  type EditProblemTestCaseContext,
  type ProblemContext,
  type ProblemTestCasesContext,
} from "routes/AppRoutes";
import type {ProblemsPageQuery} from "__generated__/ProblemsPageQuery.graphql";
import type {ProblemPageQuery} from "__generated__/ProblemPageQuery.graphql";
import type {CreateProblemPageQuery} from "__generated__/CreateProblemPageQuery.graphql";
import type {EditProblemPageQuery} from "__generated__/EditProblemPageQuery.graphql";
import type {CreateProblemTestCasePageQuery} from "__generated__/CreateProblemTestCasePageQuery.graphql";
import type {ProblemTestCasesPageQuery} from "__generated__/ProblemTestCasesPageQuery.graphql";
import type {EditProblemTestCasePageQuery} from "__generated__/EditProblemTestCasePageQuery.graphql";

// Router creation can start the initial preload, so its Relay environment must exist first.
const environment = createRelayEnvironment({
  headers: () => ({ [CsrfUtils.getHeader()]: CsrfUtils.getToken() }),
});

// Generated routes supply paths, parsers, and access-handler metadata. Authoring
// routes check /__spa/route-decision before rendering; public routes need no check.
// Return Relay references directly so spa-kit owns their disposal on route changes,
// redirects, and cancelled navigations. Preloads overlap authoring access checks.
const router = createSpaRouter(AppRoutes, {
  Home: {
    render: () => <HomePage />
  },
  Problems: {
    preload: () => loadQuery<ProblemsPageQuery>(environment, problemsPageQuery, {}),
    render: ({preload}) => <ProblemsPage queryRef={preload} />
  },
  CreateProblem: {
    preload: () => loadQuery<CreateProblemPageQuery>(environment, createProblemPageQuery, {}),
    render: ({preload}) => <CreateProblemPage queryRef={preload} />
  },
  EditProblem: {
    preload: ({params}: EditProblemContext) => loadQuery<EditProblemPageQuery>(
      environment,
      editProblemPageQuery,
      {slug: params.slug},
      {fetchPolicy: "network-only"},
    ),
    render: ({params, preload}) => <EditProblemPage key={params.slug} queryRef={preload} />,
    errorElement: <EditProblemPageError />
  },
  CreateProblemTestCase: {
    preload: ({params}: CreateProblemTestCaseContext) => loadQuery<CreateProblemTestCasePageQuery>(
      environment,
      createProblemTestCasePageQuery,
      {slug: params.slug},
      {fetchPolicy: "network-only"},
    ),
    render: ({params, preload}) => <CreateProblemTestCasePage key={params.slug} queryRef={preload} />,
    errorElement: <CreateProblemTestCasePageError />
  },
  ProblemTestCases: {
    preload: ({params}: ProblemTestCasesContext) => loadQuery<ProblemTestCasesPageQuery>(
      environment,
      problemTestCasesPageQuery,
      {slug: params.slug},
      {fetchPolicy: "network-only"},
    ),
    render: ({params, preload}) => <ProblemTestCasesPage key={params.slug} queryRef={preload} />,
    errorElement: <ProblemTestCasesPageError />
  },
  EditProblemTestCase: {
    preload: ({params}: EditProblemTestCaseContext) => loadQuery<EditProblemTestCasePageQuery>(
      environment,
      editProblemTestCasePageQuery,
      {slug: params.slug, id: params.id},
      {fetchPolicy: "network-only"},
    ),
    render: ({params, preload}) => (
      <EditProblemTestCasePage key={`${params.slug}:${params.id}`} queryRef={preload} />
    ),
    errorElement: <EditProblemTestCasePageError />
  },
  Problem: {
    preload: ({params}: ProblemContext) => loadQuery<ProblemPageQuery>(
      environment,
      problemPageQuery,
      {slug: params.slug},
    ),
    render: ({preload}) => <ProblemPage queryRef={preload} />
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

export function App() {
  return (
    <RelayRoot environment={environment} fallback={null}>
      <RouterProvider router={router} />
    </RelayRoot>
  );
}

renderComponent(<App />)

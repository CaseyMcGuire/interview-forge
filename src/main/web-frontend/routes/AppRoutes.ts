// THIS FILE IS GENERATED. DO NOT EDIT BY HAND.
// Run './gradlew generateClientRoutes' to regenerate.

type RouteMetadata = { applicationId: string; routeId: string; hasAccessHandler: boolean };

type PathDeclaration = { name: string; optional: boolean };
type QueryStringDeclaration = { name: string; optional: boolean; repeated: boolean };

// Compile-time identity only: contexts still contain just params and queryString at runtime.
declare const routeContextBrand: unique symbol;
/** Shared parser result; application code normally uses the generated per-route context aliases. */
export type ParsedRouteValues<TParams extends object, TQueryString extends object, TIdentity extends readonly [string, string]> = {
  params: TParams;
  queryString: TQueryString;
  readonly [routeContextBrand]: TIdentity;
};

function createRouteParser<TParams extends object, TQueryString extends object, TIdentity extends readonly [string, string]>(
  parameters: readonly PathDeclaration[], queryStringDeclarations: readonly QueryStringDeclaration[]
) {
  // Path values have already been decoded by the router. Invalid declared values return null.
  return (params: Readonly<Record<string, string | undefined>>, search: URLSearchParams):
    ParsedRouteValues<TParams, TQueryString, TIdentity> | null => {
    const parsedParams: Record<string, string> = Object.create(null);
    const queryString: Record<string, string | readonly string[]> = Object.create(null);
    for (const { name, optional } of parameters) {
      const value = Object.prototype.hasOwnProperty.call(params, name) ? params[name] : undefined;
      if (value === undefined) {
        if (!optional) {
          return null;
        }
      } else {
        if (typeof value !== "string") {
          return null;
        }
        parsedParams[name] = value;
      }
    }
    for (const { name, optional, repeated } of queryStringDeclarations) {
      const values = search.getAll(name);
      if (values.length === 0) {
        if (!optional) {
          return null;
        }
      } else {
        if (!repeated && values.length !== 1) {
          return null;
        }
        queryString[name] = repeated ? values : values[0];
      }
    }
    // Validation establishes the generated route's types and opaque identity.
    return { params: parsedParams, queryString } as ParsedRouteValues<TParams, TQueryString, TIdentity>;
  };
}

function routeWithoutParams<const TMetadata extends RouteMetadata>(path: string, metadata: TMetadata) {
  return Object.assign(() => path, { path, ...metadata, parse: createRouteParser<{}, {}, readonly [TMetadata["applicationId"], TMetadata["routeId"]]>([], []) });
}

function encodeRouteParam(value: string): string {
  return encodeURIComponent(value);
}

function route<TParams extends object, const TMetadata extends RouteMetadata>(path: string, buildPath: (params: TParams) => string, metadata: TMetadata, parameters: readonly PathDeclaration[]) {
  return Object.assign(buildPath, { path, ...metadata, parse: createRouteParser<TParams, {}, readonly [TMetadata["applicationId"], TMetadata["routeId"]]>(parameters, []) });
}

export const AppRoutes = {
  About: routeWithoutParams("/about", { applicationId: "app", routeId: "About", hasAccessHandler: false }),
  Blog: routeWithoutParams("/blog", { applicationId: "app", routeId: "Blog", hasAccessHandler: false }),
  CreateProblem: routeWithoutParams("/problem/create", { applicationId: "app", routeId: "CreateProblem", hasAccessHandler: true }),
  CreateProblemTestCase: route(
    "/problem/:slug/test/create",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/test/create`,
    { applicationId: "app", routeId: "CreateProblemTestCase", hasAccessHandler: true },
    [{ name: "slug", optional: false }]
  ),
  EditProblem: route(
    "/problem/:slug/edit",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/edit`,
    { applicationId: "app", routeId: "EditProblem", hasAccessHandler: true },
    [{ name: "slug", optional: false }]
  ),
  EditProblemTestCase: route(
    "/problem/:slug/test/:id/edit",
    (params: { slug: string; id: string }) => `/problem/${encodeRouteParam(params.slug)}/test/${encodeRouteParam(params.id)}/edit`,
    { applicationId: "app", routeId: "EditProblemTestCase", hasAccessHandler: true },
    [{ name: "slug", optional: false }, { name: "id", optional: false }]
  ),
  Home: routeWithoutParams("/", { applicationId: "app", routeId: "Home", hasAccessHandler: false }),
  Login: routeWithoutParams("/login", { applicationId: "app", routeId: "Login", hasAccessHandler: false }),
  Problem: route(
    "/problem/:slug",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}`,
    { applicationId: "app", routeId: "Problem", hasAccessHandler: false },
    [{ name: "slug", optional: false }]
  ),
  ProblemTestCases: route(
    "/problem/:slug/tests",
    (params: { slug: string }) => `/problem/${encodeRouteParam(params.slug)}/tests`,
    { applicationId: "app", routeId: "ProblemTestCases", hasAccessHandler: true },
    [{ name: "slug", optional: false }]
  ),
  Problems: routeWithoutParams("/problems", { applicationId: "app", routeId: "Problems", hasAccessHandler: false }),
  Register: routeWithoutParams("/register", { applicationId: "app", routeId: "Register", hasAccessHandler: false }),
} as const;

export type AppRoute = keyof typeof AppRoutes;

/** Parsed context for About; use this type on preload parameters. */
export type AboutContext = NonNullable<ReturnType<typeof AppRoutes.About.parse>>;

/** Parsed context for Blog; use this type on preload parameters. */
export type BlogContext = NonNullable<ReturnType<typeof AppRoutes.Blog.parse>>;

/** Parsed context for CreateProblem; use this type on preload parameters. */
export type CreateProblemContext = NonNullable<ReturnType<typeof AppRoutes.CreateProblem.parse>>;

/** Parsed context for CreateProblemTestCase; use this type on preload parameters. */
export type CreateProblemTestCaseContext = NonNullable<ReturnType<typeof AppRoutes.CreateProblemTestCase.parse>>;

/** Parsed context for EditProblem; use this type on preload parameters. */
export type EditProblemContext = NonNullable<ReturnType<typeof AppRoutes.EditProblem.parse>>;

/** Parsed context for EditProblemTestCase; use this type on preload parameters. */
export type EditProblemTestCaseContext = NonNullable<ReturnType<typeof AppRoutes.EditProblemTestCase.parse>>;

/** Parsed context for Home; use this type on preload parameters. */
export type HomeContext = NonNullable<ReturnType<typeof AppRoutes.Home.parse>>;

/** Parsed context for Login; use this type on preload parameters. */
export type LoginContext = NonNullable<ReturnType<typeof AppRoutes.Login.parse>>;

/** Parsed context for Problem; use this type on preload parameters. */
export type ProblemContext = NonNullable<ReturnType<typeof AppRoutes.Problem.parse>>;

/** Parsed context for ProblemTestCases; use this type on preload parameters. */
export type ProblemTestCasesContext = NonNullable<ReturnType<typeof AppRoutes.ProblemTestCases.parse>>;

/** Parsed context for Problems; use this type on preload parameters. */
export type ProblemsContext = NonNullable<ReturnType<typeof AppRoutes.Problems.parse>>;

/** Parsed context for Register; use this type on preload parameters. */
export type RegisterContext = NonNullable<ReturnType<typeof AppRoutes.Register.parse>>;

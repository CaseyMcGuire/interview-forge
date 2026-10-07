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

export const GraphiqlRoutes = {
  Graphiql: routeWithoutParams("/graphiql", { applicationId: "graphiql", routeId: "Graphiql", hasAccessHandler: false }),
} as const;

export type GraphiqlRoute = keyof typeof GraphiqlRoutes;

/** Parsed context for Graphiql; use this type on preload parameters. */
export type GraphiqlContext = NonNullable<ReturnType<typeof GraphiqlRoutes.Graphiql.parse>>;

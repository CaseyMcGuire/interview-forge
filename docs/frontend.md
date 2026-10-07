# Frontend assets and routing

## Adding routes and SPAs

1. Add a `route(...)` to [AppSpaApplication](../spa-route-definitions/src/main/kotlin/com/application/spa/AppSpaApplication.kt).
2. Run `./gradlew generateClientRoutes generateBundleEntries` (also run by `buildFrontend` and `watchFrontend`).
3. Add a matching key with a `render` callback to [App.tsx](../src/main/web-frontend/App.tsx)'s `createSpaRouter(AppRoutes, ...)` configuration. Generated routes provide paths, IDs, and parameter parsers; TypeScript requires a configuration for every route.

The spa-routing starter registers the server GET mapping automatically. To add a separate SPA, define a `SinglePageApplicationDefinition` in `spa-route-definitions/` and expose its generated `<Name>ApplicationConfig` as a Spring bean. Supply its application access handler, generated route-handler collection, and `HtmlRenderer`. The definition supplies its frontend entry and routes; no Vite input or controller changes are needed. Definitions are codegen inputs only and are not on the server runtime classpath.

Application access handlers explicitly return `AccessDecision.Allowed` for these public SPAs. Mark restricted routes with `generateAccessHandler = true` and supply implementations of the generated handlers in `AppRouteAccessHandlers`. The five authoring handlers share `RequireAdminRouteAccess`, which reads the current database role and returns typed login or problems destinations when access is denied.

`createSpaRouteDecisionMiddleware`, registered once in `sharedMiddleware`, checks `/__spa/route-decision` before navigation to routes with `hasAccessHandler`. Public routes skip the endpoint; initial page loads still run application access. Decisions are `{ "type": "allowed" }` or a failure (`denied`, `unknown_route`, `invalid_request`) with `destination`. Unknown or malformed requests recover to the existing home route through `DefaultRouteFailureHandler`; transport errors use the server's `/error` page.

## JavaScript and HTML

[ReactPage.kt](../src/main/kotlin/com/application/views/ReactPage.kt) renders the HTML shell with fixed asset paths. [vite.config.ts](../vite.config.ts) pins output filenames; keep both sides aligned when renaming assets.

Route codegen produces `SinglePageApplicationBundles.ts`, the Vite input map. Each bundle is served as `/bundles/<id>.bundle.js`; each application config supplies an HTML renderer delegating to `ReactPage`. The starter owns the HTTP response.

React and React DOM resolve through ReactPage's esm.sh import map, including `react/compiler-runtime`. Keep its version aligned with `package.json`. Rolldown's `esmExternalRequirePlugin` converts CommonJS React requires inside dependencies such as Relay into imports; preserve this when changing bundler configuration.

Keep `babel-plugin-react-compiler` first in the Babel pipeline, before Relay's GraphQL rewrite and the React/StyleX transforms.

## CSS

- App-wide StyleX rules and the global reset at `src/main/web-frontend/styles.css` become `/bundles/stylex.generated.css` through Vite's `stylexCssFile` plugin. ReactPage links it on every page. The reset is inlined at build time; don't import it from TypeScript or split StyleX CSS per page.
- Entry-specific CSS needs an explicit stylesheet link in the SPA configuration's `HtmlRenderer` using `ReactPage.customHead`. Entry JavaScript does not load it. See [GraphiqlSpaConfig](../src/main/kotlin/com/application/config/GraphiqlSpaConfig.kt) for `/bundles/graphiql.css`.
- Vite injects CSS from lazily imported chunks at runtime; it needs no manual link.

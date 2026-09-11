# Frontend assets and routing

## Adding routes and SPAs

1. Add a `route(...)` to [AppSpaApplication](../spa-route-definitions/src/main/kotlin/com/application/spa/AppSpaApplication.kt).
2. Run `./gradlew generateClientRoutes generateBundleEntries` (also run by `buildFrontend` and `watchFrontend`).
3. Use the generated `AppRoutes.<Name>.routeId` and `.path` in [App.tsx](../src/main/web-frontend/App.tsx)'s route list and assign the page component.

The spa-routing starter registers the server GET mapping automatically. To add a separate SPA, define a `SpaApplicationDefinition` in `spa-route-definitions/` and a `SinglePageApplicationConfig` Spring component. The definition supplies its frontend entry and routes; no Vite input or controller changes are needed.

`withRouteAuthorization` and `spaRoutingResolver` check `/__spa/route-decision` before in-page navigation. Declare access rules in the SPA's server configuration. Empty application rules deny every route; `AllowAll()` permits public access. Use appropriate `SpaRouteRule`s to restrict pages.

## JavaScript and HTML

[ReactPage.kt](../src/main/kotlin/com/application/views/ReactPage.kt) renders the HTML shell with fixed asset paths. [vite.config.ts](../vite.config.ts) pins output filenames; keep both sides aligned when renaming assets.

Route codegen produces `SinglePageApplicationBundles.ts`, the Vite input map. Each bundle is served as `/bundles/<id>.bundle.js`; the starter renders it through `AppSpaHtmlRenderer` and `ReactPage`.

React and React DOM resolve through ReactPage's esm.sh import map, including `react/compiler-runtime`. Keep its version aligned with `package.json`. Rolldown's `esmExternalRequirePlugin` converts CommonJS React requires inside dependencies such as Relay into imports; preserve this when changing bundler configuration.

Keep `babel-plugin-react-compiler` first in the Babel pipeline, before Relay's GraphQL rewrite and the React/StyleX transforms.

## CSS

- App-wide StyleX rules and the global reset at `src/main/web-frontend/styles.css` become `/bundles/stylex.generated.css` through Vite's `stylexCssFile` plugin. ReactPage links it on every page. The reset is inlined at build time; don't import it from TypeScript or split StyleX CSS per page.
- Entry-specific CSS needs an explicit stylesheet link in the SPA configuration's `renderHtml()` using `ReactPage.customHead`. Entry JavaScript does not load it. See [GraphiqlSpaConfig](../src/main/kotlin/com/application/config/GraphiqlSpaConfig.kt) for `/bundles/graphiql.css`.
- Vite injects CSS from lazily imported chunks at runtime; it needs no manual link.

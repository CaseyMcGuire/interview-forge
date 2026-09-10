# AGENTS.md

Template repo for full-stack web apps: Kotlin/Spring Boot backend serving a GraphQL API (Netflix DGS) to a React/Relay frontend, with Postgres behind EntKt and Flyway migrations. The Gradle build orchestrates everything, including the frontend (via the node-gradle plugin, which downloads its own Node/npm).

## Commands

| Task | Command |
|---|---|
| Run the app (full build, then serves on `localhost:8080`) | `./gradlew bootRun` |
| Frontend watch mode (rebuild + typecheck on change, refresh browser) | `./gradlew watchFrontend` |
| Rebuild Relay artifacts after changing a query/fragment | `./gradlew buildRelay` (or `npm run relay-compiler`) |
| Typecheck frontend | `npm run typecheck` |
| Production frontend bundle (typecheck + Vite build) | `npm run build` |
| Backend tests | `./gradlew test` — requires Docker (Testcontainers spins up `postgres:18.6-alpine`) |
| Apply DB migrations without starting the app | `./gradlew flywayMigrate` |
| Regenerate EntKt entities and client | `./gradlew generateEntkt` (also runs before backend compilation) |
| Validate EntKt schema definitions | `./gradlew validateEntSchemas` |

Prerequisites: JDK 26 (also used by the Gradle daemon) and a `.env` file in the repo root with `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_URL_PREFIX` — `build.gradle.kts` reads it eagerly, so **all Gradle commands fail without it**. `./bin/setup_database` creates the database.

EntKt is pinned by `entktVersion` in `gradle.properties` and resolves from Maven Central. The spa-routing 0.3.0 Gradle plugin must be published to Maven local before building (see README.md).

## Layout

- `src/main/kotlin/com/application/` — backend. The build assumes exactly one folder under `src/main/kotlin/com/` (codegen paths are derived from it). Subpackages: `controllers`, `services`, `dao`, `graphql` (DGS data fetchers), `views` (kotlinx.html server-side pages), `config`, `db`.
- `src/main/web-frontend/` — frontend (TypeScript, React 19 with React Compiler, Relay, React Router, StyleX via the `sx` prop, shared `@spa-kit/*` packages). Bundled by Vite (`vite.config.ts` at repo root) into `src/main/resources/static/bundles/`. See "How pages get their assets" below.
- `src/main/resources/schema/` — the GraphQL schema, split across multiple `.graphql` files. This is the single source of truth for both codegen pipelines below.
- `src/main/resources/db/migration/` — Flyway migrations, named `V<N>__description.sql` with `N` incrementing.
- `spa-route-definitions/` — Gradle subproject holding the `SpaApplicationDefinition` objects: the single source of truth for every SPA's bundle and routes (see "How pages get their assets"). Separate module so spa-routing codegen doesn't cycle with the root `compileKotlin`.
- `ent-schema/` — EntKt entity definitions, compiled separately so codegen can run before the backend compiles. Currently maps only `users`; the existing `posts` table is preserved by Flyway but has no EntKt schema yet.
- `.agent/adr/` — architecture decision records: what was chosen, why, and the trade-offs. Read before proposing a change to the stack; add a numbered ADR when making one.

## How pages get their assets

There is no HTML plugin or Vite manifest: `ReactPage.kt` renders the HTML shell and references build outputs **by fixed name**, so the Vite config pins output filenames (`entryFileNames`/`assetFileNames`) and the two sides must stay in sync.

- **Entries**: each SPA is declared as a `SpaApplicationDefinition` in `spa-route-definitions/` (the single source of truth for both bundles and URLs). Codegen turns those into the Vite input map (`SinglePageApplicationBundles.ts`) and typed client route builders (`src/main/web-frontend/routes/`); the spa-routing Spring Boot starter registers a server GET route per defined route, rendered by `AppSpaHtmlRenderer` via `ReactPage`. Bundles are emitted as `/bundles/<id>.bundle.js`.
- **react is not bundled**: `react`/`react-dom` stay external (via Rolldown's `esmExternalRequirePlugin`, which also converts CommonJS `require("react")` calls inside deps like react-relay into imports) and resolve in the browser through the esm.sh import map that `ReactPage.kt` emits. This includes React Compiler's React 19 runtime at `react/compiler-runtime`.
- **React Compiler**: `babel-plugin-react-compiler` runs first in the `@rolldown/plugin-babel` pipeline, before Relay rewrites GraphQL tags and before the React/StyleX transforms.
- **App-wide CSS**: all compiled StyleX rules plus the global reset (`web-frontend/styles.css`, inlined at build time — it is not imported from TypeScript) are emitted as `/bundles/stylex.generated.css` by the `stylexCssFile` plugin in `vite.config.ts`. `ReactPage.kt` links it on every page. StyleX CSS cannot be split per-page by design.
- **Entry-specific CSS**: if an entry's imports bundle CSS (e.g. GraphiQL's `graphiql/style.css` → `/bundles/graphiql.css`), the entry JS does **not** load it — the SPA's `SinglePageApplicationConfig` must link it by overriding `renderHtml()` with a `ReactPage.customHead { link(...) }` (see `GraphiqlSpaConfig`). CSS of *lazily imported* chunks (e.g. monaco) is injected at runtime by Vite and needs no linking.
- **Adding a client route to the main app**: add a `route(...)` to `AppSpaApplication` in `spa-route-definitions/`, run `./gradlew generateClientRoutes generateBundleEntries` (the `buildFrontend`/`watchFrontend` tasks do this automatically), then reference the generated `AppRoutes.<Name>` entry in `App.tsx`'s route list. The server GET mapping appears automatically.
- **Adding a whole new SPA**: add a `SpaApplicationDefinition` object in `spa-route-definitions/` plus a `SinglePageApplicationConfig` `@Component` — no vite.config or controller changes needed.
- **Route authorization**: `App.tsx` wraps its routes in `withRouteAuthorization` + `spaRoutingResolver` (from `@spa-kit/react-router`), which asks `/__spa/route-decision` before each in-page navigation. Application-level rules are a **deny-by-default gate** (spa-routing 0.2.0): an SPA with no rules 404s on every route, so each `SinglePageApplicationConfig` declares `AllowAll()` as the explicit ungated opt-in. Replace it with real `SpaRouteRule`s (e.g. require-login) to gate pages without client changes.

## The four codegen pipelines

1. **DGS (server)**: Gradle's DGS codegen generates Kotlin types from `src/main/resources/schema/` into `build/generated` (package `com.application.graphql`). Runs as part of the build; not committed.
2. **Relay (client)**: `npm run relay-compiler` runs `spa-kit-compile-relay` (from `@spa-kit/node`), which combines the split schema files into a transient `src/main/resources/relay/schema.graphql`, runs `relay-compiler` against it, then deletes it. Artifacts land in `src/main/web-frontend/__generated__/` and **are committed** — rerun after any GraphQL query/fragment/schema change and commit the result. Relay config lives in the `"relay"` key of `package.json`.
3. **EntKt (database)**: `./gradlew generateEntkt` compiles the schemas in `ent-schema/` and generates entities, typed query/mutation APIs, and `EntClient` into `build/generated/entkt` (package `com.application.ent`). Runs before backend compilation; not committed and needs no live database. Flyway SQL remains authoritative for database changes; edit both the entity definition and a new migration when changing storage.
4. **spa-routing (routes)**: the `io.github.caseymcguire.spa-routing` Gradle plugin reads the `SpaApplicationDefinition`s in `spa-route-definitions/` and generates the Vite input map (`SinglePageApplicationBundles.ts`, **committed**), typed TS route builders (`src/main/web-frontend/routes/`, **committed** — kept outside `__generated__` because the Relay compiler deletes unexpected files there), and typed Kotlin route objects (`build/generated`, not committed). `buildFrontend`/`watchFrontend` regenerate the first two automatically.

Never hand-edit generated code (`build/generated/`, `__generated__/`, `routes/`, `SinglePageApplicationBundles.ts`).

## Database access

- `DatabaseConfiguration` supplies an `EntClient` using Spring's `DataSource` and `PostgresDriver(autoDdl = false)`. Flyway applies SQL; EntKt never creates or alters tables on startup.
- `UserPolicy` explicitly permits public registration (`create(allowAll)`). `UserDao.createUser` saves with an anonymous viewer, without loading the credential entity. Read/update/delete remain denied to ordinary viewers. Only `UserDao.findByEmail` uses a private, explicitly justified privacy bypass because login must read credentials before authentication; do not reuse this bypass for mutations or API-facing queries.
- Use EntKt's `withTransaction { tx -> ... }` for multi-operation transactions and use the supplied `tx` client inside the block. EntKt does not participate in Spring `@Transactional` through its default Postgres driver.
- Exposed, jOOQ, and their generators have been removed. Write Flyway migrations directly for now; EntKt's migration plugin is not configured while the schemas cover only part of the database.

## Workflow conventions

- Commit directly to `master` and push; do not create feature branches or PRs.
- After database changes, update `ent-schema/` and add a Flyway migration, then run `flywayMigrate` and rebuild (EntKt generation runs automatically). After GraphQL schema edits, the server rebuild picks up DGS types; run `buildRelay` for the client and commit its artifacts.
- Frontend deps are managed in root `package.json`; the checked-in `package-lock.json` matters because Gradle runs `npm install` during `bootRun` builds.

# Architecture Decision Records

This directory records the architectural decisions baked into this template and the reasoning behind them. When you make a new significant decision (or reverse one below), add a numbered file (`NNNN-short-title.md`) here and list it in the index at the bottom. The baseline decisions that shaped the template are summarized inline below.

## 1. Kotlin + Spring Boot server, Gradle orchestrates everything

The server is Kotlin on Spring Boot. Gradle is the single build entry point: it downloads its own Node/npm (node-gradle plugin), runs `npm install`, bundles the frontend, applies database migrations, and boots the app — `./gradlew bootRun` brings up the whole stack with no separately-installed frontend toolchain.

**Trade-off:** Gradle config carries frontend concerns (pinned Node/npm versions in `build.gradle.kts` must satisfy the frontend tooling's engine requirements, e.g. Vite needs Node ≥22.12).

## 2. GraphQL API via Netflix DGS, schema-first and split across files

The API is GraphQL served by DGS. The schema is the single source of truth, split across multiple `.graphql` files in `src/main/resources/schema/`, from which both server types (DGS codegen) and client artifacts (Relay) are generated.

**Trade-off:** Relay's compiler wants one schema file, so the build recombines the split files transiently (`spa-kit-compile-relay` from `@spa-kit/node`).

## 3. Postgres with jOOQ + Exposed; no JPA/Hibernate

**Superseded by [0012: EntKt for user data access](0012-entkt-user-data-access.md).**

Data access uses jOOQ (typesafe SQL, models generated from the live database via `./gradlew generateJooq`) and Exposed, over plain `spring-boot-starter-jdbc`. Spring Data JPA/Hibernate is deliberately excluded as dead weight. Migrations are Flyway, applied on startup.

**Trade-off:** schema changes require a codegen step (`flywayMigrate` → `generateJooq`) and the generated models are committed.

## 4. Relay for client data fetching

The client uses Relay (not Apollo/urql/fetch): compiled queries, typed artifacts committed under `__generated__/`, and fragments colocated with components. `./gradlew buildRelay` regenerates artifacts after any query/schema change.

**Trade-off:** an extra compile step and CommonJS packages (`react-relay`, `relay-runtime`) that the bundler must accommodate (see decision 6).

## 5. Server-rendered HTML shell via kotlinx.html; assets referenced by fixed name

Pages are served by Spring controllers rendering a kotlinx.html shell (`ReactPage.kt`) — there is no HTML plugin, no Vite manifest, and no Node server. The shell references bundles by fixed, unhashed names (`/bundles/<entry>.bundle.js`, `/bundles/stylex.generated.css`), so the Vite config pins output filenames and the two sides must stay in sync. See "How pages get their assets" in AGENTS.md.

**Trade-off:** no content-hash cache busting on asset filenames, and renames must be made on both sides.

## 6. React is not bundled — loaded via an esm.sh import map

`react`/`react-dom` are externals resolved in the browser through the import map `ReactPage.kt` emits (pinned to esm.sh URLs). Bundles stay smaller and every page shares one copy of React. Rolldown's `esmExternalRequirePlugin` bridges the CJS `require("react")` calls inside relay packages to imports.

**Trade-off:** runtime dependency on esm.sh (or whatever the import map points at), and the React version is pinned in `ReactPage.kt` separately from `package.json`.

## 7. Vite (Rolldown) as the bundler

The frontend is bundled by Vite 8, migrated from webpack in July 2026 for ecosystem longevity and build speed (~1s vs ~30s). Relay's babel transform runs via an inline plugin in `vite.config.ts`; `vite-plugin-checker` typechecks during watch mode; production builds run `tsc` explicitly.

**Trade-off:** Rolldown is newer than Rollup — two small config workarounds exist (`esmExternalRequirePlugin` for CJS externals, `stableCssNames` for the StyleX plugin's asset re-emission).

## 8. StyleX for styling, emitted as one app-wide stylesheet

Component styles use StyleX (via the `sx` prop). StyleX compiles all atomic rules across the app into a single deduplicated, globally-ordered payload — it cannot be split per page — so the build emits it (plus the global reset from `styles.css`) as `/bundles/stylex.generated.css`, linked on every page. Entry-specific CSS (e.g. GraphiQL's) is linked explicitly by the page's controller via `customHead`.

**Trade-off:** every page downloads all StyleX rules (small, atomic CSS dedupes heavily).

## 9. Shared SPA plumbing lives in @spa-kit packages

Generic frontend/build plumbing (Relay environment setup, page rendering helpers, the schema-combining relay compile CLI) is extracted to the published `@spa-kit/*` packages (developed in the sibling `spa-utils` repo) rather than duplicated per project.

**Trade-off:** template upgrades sometimes require publishing a package first, and peer-dependency ranges must track dependency bumps (e.g. `@spa-kit/react` pins the StyleX major).

## 10. Routes defined once, in Kotlin, via spa-routing

SPA routes live in `SpaApplicationDefinition` objects in the `spa-route-definitions/` Gradle
subproject — a single source of truth from which the `io.github.caseymcguire.spa-routing`
Gradle plugin generates the Vite entry map, typed TypeScript route builders, and typed Kotlin
route objects, while its Spring Boot starter registers a server GET mapping per route (making
deep links and reloads work without hand-maintained controllers). The client wraps its
react-router routes in `withRouteAuthorization`/`spaRoutingResolver` (`@spa-kit/react-router`),
which consults `/__spa/route-decision` so server-declared route rules gate in-page navigation
too. Replaced a hand-written `MainController` whose path list silently had to mirror App.tsx.

**Trade-off:** a codegen step between editing routes and using them, and the spa-routing
artifacts are currently published to mavenLocal only, so building this template requires them
installed locally.

## 11. Cookie-based CSRF (double-submit) for the SPA

Spring Security uses `CookieCsrfTokenRepository` (non-HttpOnly) with the plain request-attribute handler, and `CsrfCookieFilter` forces the deferred token to be written on every response so the first page load already carries the `XSRF-TOKEN` cookie. The client echoes it back as `X-XSRF-TOKEN` (`CsrfUtils.ts`).

## Index

- [0012: EntKt for user data access](0012-entkt-user-data-access.md)

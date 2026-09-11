# AGENTS.md

Kotlin/Spring Boot + DGS GraphQL, React/Relay, Postgres, EntKt, and Flyway. Gradle builds both server and frontend and downloads Node/npm.

## Task scope

- Implement only what the user asks for, including the minimum supporting changes, required code generation, and focused validation needed for that request.
- Broader project goals and earlier discussions provide context; they do not authorize implementing the next feature. Do not add adjacent functionality, refactor unrelated code, or introduce dependencies for work the user has not requested.
- For example, a request to add a route through `spa-route-definitions` means adding the route definition and regenerating its artifacts. It does not authorize building the page, connecting GraphQL data, changing the editor, or adding a Markdown dependency.
- If the scope is ambiguous, prefer the smallest reasonable interpretation and ask before expanding it. Once the requested change is complete, stop and let the user decide the next step.

## Commands and setup

| Task | Command |
|---|---|
| Run app on `localhost:8080` | `./gradlew bootRun` |
| Watch frontend; refresh browser after rebuild | `./gradlew watchFrontend` |
| Generate Relay artifacts | `./gradlew buildRelay` |
| Typecheck frontend | `npm run typecheck` |
| Production frontend build | `npm run build` |
| Backend tests (requires Docker) | `./gradlew test` |
| Apply migrations | `./gradlew flywayMigrate` |
| Generate EntKt code / validate schemas | `./gradlew generateEntkt` / `./gradlew validateEntSchemas` |

All Gradle commands require a root `.env` containing `DB_USER`, `DB_PASSWORD`, `DB_NAME`, and `DB_URL_PREFIX`. Use plain `KEY=value` lines without blanks, comments, quotes, or `=` inside values. For prerequisites and fresh-clone setup, read [setup-project](.agents/skills/setup-project/SKILL.md).

## Changes and generated code

- Commit directly to `master` and push; do not create feature branches or PRs.
- Database changes: update `ent-schema/`, add the next `V<N>__description.sql` under `src/main/resources/db/migration/`, run `flywayMigrate`, then rebuild. Flyway owns physical schema changes; keep EntKt automatic DDL disabled.
- GraphQL schema lives in `src/main/resources/schema/`. After schema changes, rebuild the server for DGS types. After schema, query, or fragment changes, run `buildRelay` and commit its artifacts.
- Routes and bundle entries originate in `spa-route-definitions/`. Run `./gradlew generateClientRoutes generateBundleEntries` after edits; `buildFrontend` and `watchFrontend` also regenerate them.
- Never hand-edit generated files. Commit Relay's `src/main/web-frontend/__generated__/`, client `src/main/web-frontend/routes/`, and `SinglePageApplicationBundles.ts`. Keep routes outside `__generated__/` because Relay deletes unexpected files there. DGS, EntKt, and Kotlin route outputs under `build/generated/` are not committed.
- Frontend dependencies live in root `package.json`; commit corresponding `package-lock.json` changes.
- Keep exactly one application directory under `src/main/kotlin/com/`; codegen derives package paths from it.
- Before changing the stack, read the relevant [ADRs](.agent/adr/README.md); add a numbered ADR when making an architectural decision.

## Database access

- Public registration uses `UserPolicy`'s create permission and an anonymous viewer, without loading credentials. Ordinary viewers cannot read, update, or delete credential entities. Keep the privacy bypass confined to `UserDao.findByEmail` for authentication; never reuse it for mutations or API queries.
- Use EntKt `withTransaction { tx -> ... }` and the supplied client for multi-operation transactions. Its default Postgres driver does not join Spring `@Transactional`.

## How pages get their assets

Keep Vite's fixed output filenames aligned with `ReactPage.kt`, and its React import-map version aligned with `package.json`. SPA authorization denies access when no rules are declared; `AllowAll()` explicitly permits public access.

Read [frontend assets and routing](docs/frontend.md) when changing bundles, CSS, or routes.

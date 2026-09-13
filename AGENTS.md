# AGENTS.md

Kotlin/Spring Boot + DGS GraphQL, React/Relay, Postgres, EntKt, and Flyway. Gradle builds both server and frontend and downloads Node/npm.

## Shared instructions

Before starting work, fetch and read [CaseyMcGuire/agent-config's AGENTS.md](https://github.com/CaseyMcGuire/agent-config/blob/master/AGENTS.md). Follow its workflow, general conventions, and applicable linked language and library conventions. Resolve remote links within that repository. If any referenced instructions cannot be retrieved, report the unavailable file or URL.

The project-specific instructions below override conflicting shared defaults.

## Local setup

For prerequisites, setup, and run commands, read [setup-project](.agents/skills/setup-project/SKILL.md). Backend integration tests require Docker.

All Gradle commands require a root `.env` containing `DB_USER`, `DB_PASSWORD`, `DB_NAME`, and `DB_URL_PREFIX`. Use plain `KEY=value` lines without blanks, comments, quotes, or `=` inside values.

## Changes and generated code

- When asked to commit and push, use `master` by default. Create feature branches or PRs only when explicitly requested.
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

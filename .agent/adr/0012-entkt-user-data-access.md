# 0012: EntKt for user data access

Date: 2026-09-09

Status: Accepted. Supersedes baseline decision 3.

## Context

The only active data access was `UserDao`, implemented with Exposed. jOOQ was configured for
code generation but had no application callers. Adopt EntKt starting with users, remove Exposed,
and avoid retaining a second unused data access library.

## Decision

- Declare `User` in the independent `ent-schema` module. Keep the existing `users` table name,
  database-generated long id, email uniqueness, and hashed password column.
- Use the EntKt Gradle plugin to generate the typed entities and client under `build/generated/entkt`
  before compilation. Pin all EntKt artifacts and the plugin to `entktVersion` in `gradle.properties`.
- Wire `EntClient` to Spring's pooled `DataSource` with `PostgresDriver(autoDdl = false)`.
  Move `UserDao` to generated queries and mutations while preserving its service contract.
- Mark the password hash sensitive. Explicitly allow public creation in `UserPolicy`; registration
  saves with an anonymous viewer and does not request a returned entity, so it needs no LOAD
  permission or privacy bypass. Only credential lookup uses a private, justified privacy bypass:
  Spring Security must look up a password hash before the caller is authenticated. Ordinary
  viewers cannot read, update, or delete credential entities.
- Keep the existing Flyway SQL unchanged. The `posts` table is preserved, but adding its EntKt
  schema is a separate step. Write migrations directly rather than enabling whole-database
  EntKt migration planning with only a user schema.
- Remove Exposed, jOOQ, the custom jOOQ generator module, and the Exposed migration-script task.

## Consequences

Data access uses one framework. Building entities no longer needs a live database, and generated
Kotlin is disposable build output. Flyway still owns the physical schema, including the existing
`VARCHAR(255)` constraints that EntKt's generic string declaration does not describe exactly.
Schema definitions and SQL migrations must be kept consistent manually during partial adoption.

EntKt is currently consumed from Maven local, like spa-routing. Developers need to publish the
pinned EntKt version locally before building this template. Its Postgres driver owns its transaction
connections; use `EntClient.withTransaction` and its supplied client for atomic multi-operation work,
not Spring's `@Transactional` alone.

Integration tests exercise the unchanged migrations in PostgreSQL, user creation and lookup,
registration policies, duplicate-email constraints, password hashing and credential loading, and
denial of ordinary credential reads.

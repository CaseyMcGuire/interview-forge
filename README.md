# Full Stack Spring Boot/React/GraphQL Template

This is a project template I use for creating new web applications. It uses the following technologies
- [Kotlin](https://kotlinlang.org/) as the server-side language of choice
- [Spring Boot](https://spring.io/projects/spring-boot) as the web application framework 
- [kotlinx.html](https://github.com/kotlin/kotlinx.html) for server-side HTML rendering.
- [Postgres](https://www.postgresql.org/) as the database
- [EntKt](https://github.com/CaseyMcGuire/EntKt) for typed entities, queries, and mutations.
- [Flyway](https://flywaydb.org/) for handling database migrations.
- [GraphQL](https://graphql.org/) as the API query language (using [Netflix DGS](https://netflix.github.io/dgs/))
- [TypeScript](https://www.typescriptlang.org/) as the client-side language of choice
- [React](https://react.dev/) as the UI rendering library.
- [Relay](https://relay.dev/) as the client-side data fetching API.
- [Stylex](https://stylexjs.com/docs/learn/) for client-side styling
- [React Router](https://reactrouter.com/en/main) as the client-side routing framework
- [Vite](https://vite.dev/) for bundling the client-side code
- spa-routing (Kotlin/Gradle/Spring) and `@spa-kit/*` (npm) — my shared libraries for wiring single-page apps into a Spring backend (see below)

## Setup (for Mac)

### Install Java and local Kotlin libraries

Install JDK 26. Gradle's checked-in daemon criteria and all modules use Java 26. The Gradle
wrapper downloads Gradle 9.7.1, and the build downloads Node 26.8.2 and npm 12.0.2 automatically.

EntKt is pinned in `gradle.properties` (`0.1.0-alpha.1`, its latest published release; no stable
release is available yet). Its artifacts are available from Maven Central.

The spa-routing 0.3.0 Gradle plugin still needs to be installed in Maven local. From a
spa-routing checkout at version `0.3.0`, run `./gradlew publishToMavenLocal`.

GraphQL.js stays on the latest 16.x patch because `@spa-kit/node` and `@spa-kit/react-relay`
still require that major. Other transitive backend libraries follow the Spring Boot and DGS BOMs.

### Setup database

1) Install [Postgres](https://www.postgresql.org/download/).
2) Create a `.env` file in the project root directory (use `.env.example` as an example)
3) Set the `DB_USER`, `DB_PASSWORD`, and `DB_NAME` variables in your `.env` file as your database username, password, name, respectively.
4) Run `./bin/setup_database` in the root of the project.
5) (Optional) The variable `DB_URL_PREFIX` is set to default Postgres database URL is `jdbc:postgresql://localhost:5432/` but it can be changed. The application assumes that the URL to connect to the database will be `DB_URL_PREFIX` concatenated with `DB_NAME`, where `DB_NAME` is the name of the database specified above (you can read more about connecting to a Postgres database [here](https://www.postgresql.org/docs/6.4/jdbc19100.htm#:~:text=Defaults%20to%20%22localhost%22.)). For example, if your `DB_NAME` variable is `test_db`, then the URL will be assumed to be `jdbc:postgresql://localhost:5432/test_db`.

- For example, suppose our user was named `test_user`, our password `test_password`, and `DB_NAME` was `test_database`, then our `.env` would something like this:
```
DB_USER=test_user
DB_PASSWORD=test_password
DB_NAME=test_database
DB_URL_PREFIX=jdbc:postgresql://localhost:5432/ # keep the default
```

### How to run
In order to start:
```
./gradlew bootRun
```
and navigate to `localhost:8080` in your web browser. 

This command will: 
1. Compile all server-side code.
2. Run any pending database migrations
3. Install or update `node` and `npm`, if necessary
4. Runs `npm install` to get latest node dependencies defined in `package.json`
5. Runs Vite to compile and bundle client-side code
6. Starts the server on `localhost:8080`.

If you make client-side changes and want to see them without restarting the server, you can run Vite in watch mode. In order to do so, open a new terminal and run the following:
```
./gradlew watchFrontend
```
Vite watches the client-side directories and rebuilds the bundles in `build`. Each rebuild runs the TypeScript 7 compiler to report type errors. Refresh the page to see changes.

---
Whenever you change a client-side GraphQL query supported by Relay, you must rebuild the Relay models. In order to do so, run the following: 

```
./gradlew buildRelay
```
---
# Changing the database
### Adding a database migration

In order to change the database, you must add a new migration. Since we use Flyway for migrations, you can read about how to structure and run repeatable versus versioned migrations [here](https://documentation.red-gate.com/flyway/flyway-cli-and-api/concepts/migrations). 

For simplicity, we'll assume we want to run a versioned migration. From the Flyway link above:

> Versioned migrations have a version, a description and a checksum. The version must be unique. The description is purely informative for you to be able to remember what each migration does. The checksum is there to detect accidental changes. Versioned migrations are the most common type of migration. They are applied in order exactly once.
>
> Each versioned migration must be assigned a unique version. Any version is valid as long as it conforms to the usual dotted notation. For most cases a simple increasing integer should be all you need[...]
>
>Versioned migrations are applied in the order of their versions. Versions are sorted numerically as you would normally expect.

That is, the file name of the SQL file should be `<VERSION>__<short_description>.sql`. Since migrations are run in the sorted numerical order of their filenames, you should look at the version number of the last migration and increment it by 1. For example, if the first migration was named `V1__initial_setup.sql`, the second migration would be something like `V2__add_new_column.sql` and the third one could be `V3__add_second_table.sql`. 


For this project, migrations are stored in `src/main/resources/db/migration` but this can be configured.

### Running a database migration
There are two ways to run all pending migrations:

1. Start the application. 
    - By default, Spring automatically runs Flyway migrations on application startup (see [here](https://docs.spring.io/spring-boot/docs/2.0.0.M5/reference/html/howto-database-initialization.html#howto-execute-flyway-database-migrations-on-startup))
2. Run `./gradlew flywayMigrate` from the application root.

### EntKt entities

Entity definitions live in the separate `ent-schema/` Gradle module. The initial `User` schema maps
the existing `users` table: its `BIGSERIAL` id, unique email, and `hashed_password` column. Password
hashes are marked sensitive so generated entity string representations omit them. The original
Flyway migrations and their `VARCHAR(255)` constraints remain unchanged. The `posts` table remains
in the database but does not yet have an EntKt schema.

`./gradlew generateEntkt` generates `com.application.ent` into `build/generated/entkt`, including
`User` and `EntClient`. Backend compilation runs this automatically; generated files are not
committed. `./gradlew validateEntSchemas` checks the definitions without a running database.

When changing storage, edit the EntKt definition and add a new Flyway SQL migration. Flyway remains
the authority for physical column types and constraints: EntKt's `string` fields can read the
existing varchar columns, but its default DDL describes them as text. Automatic DDL and EntKt's
Flyway migration generator are not enabled for this partial schema adoption.

`UserDao` uses the generated client for registration and credential lookup. `UserService` still
hashes passwords, and Spring Security handles login. `UserPolicy` explicitly permits public creation;
registration saves with an anonymous viewer and does not load the credential entity. Only the internal
password lookup bypasses entity privacy because it happens before authentication. Ordinary viewers
cannot read, update, or delete credential entities. For multi-operation transactions, use
`EntClient.withTransaction` and the client passed into its block; the default driver does not join
Spring `@Transactional` scopes.

Exposed and jOOQ, including their migration/codegen tooling, have been removed. Run `./gradlew test`
with Docker running to check user persistence, registration policies, and credential loading
against a disposable PostgreSQL database.

## How spa-routing and spa-kit work together

The client router, the server's GET mappings, the bundler's entry points, and per-page auth checks
all describe the same set of routes — and drift apart when maintained by hand. Two shared libraries
keep them in sync:

- **spa-routing** (Kotlin) — routes are defined once, as `SpaApplicationDefinition`s in
  `spa-route-definitions/`. Its Gradle plugin generates the Vite entry map and typed route builders
  for both languages, and its Spring Boot starter serves a GET mapping per route (so deep links and
  reloads just work) plus `/__spa/route-decision` for evaluating server-declared route rules.
- **spa-kit** (`@spa-kit/*` on npm) — the client runtime. `App.tsx` builds its react-router routes
  from the generated builders, and `@spa-kit/react-router` checks each navigation against
  `/__spa/route-decision`, so rules like "require login" are declared once on the server and
  enforced on direct loads and in-page navigations alike.

Adding a route is one edit to the route definition; the codegen and starter keep everything else in
step. See AGENTS.md for the mechanics.

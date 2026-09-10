---
name: setup-project
description: Set up a fresh clone of this Kotlin/Spring Boot, EntKt, React/Relay template for local development, including prerequisites, database configuration, dependencies, and a working app. Use for onboarding or fixing local setup, not dependency upgrades or deployment.
---

# Set Up Project

Bring the requested checkout to a working local app. Run the setup, rather than only listing commands, unless the user asks for instructions.

## Read the checkout

- Work from the requested repo root. Read `AGENTS.md`, the setup section of `README.md`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/gradle-daemon-jvm.properties`, and `bin/setup_database` as needed.
- Take Java, Node/npm, dependency, and test-container versions from the checkout; do not substitute the newest releases. The current build downloads Gradle and Node/npm itself. Java must be installed and discoverable by Gradle.

## Configure local prerequisites

- Reuse a suitable installed JDK and existing PostgreSQL service. If prerequisites are absent, install the pinned JDK and a supported PostgreSQL version through official distributions or the machine's package manager, within the available execution permissions. Confirm PostgreSQL accepts connections and `psql` is available before provisioning.
- Docker is needed for the Testcontainers integration tests, not for running against a native PostgreSQL service. An existing Docker installation can also host a dedicated local development database when that fits the user's setup; keep it bound to localhost and retain its data volume.
- Honor an existing `.env` and database. Do not replace credentials, recreate an existing database, or stop unrelated services. If the configured database is nonlocal, establish that it is the intended development target before running startup migrations.

## Create or validate `.env`

All Gradle tasks read `.env` during configuration, including tasks that do not use a database. Create it before invoking Gradle.

The current parser splits every line on `=`. It does not support blank lines, comments, quoting, or values containing `=`; shell expansion is also unsupported. Inspect existing configuration without printing passwords. If the parser has changed in the checkout, follow its actual behavior.

For a missing `.env`, choose simple project-specific role/database names. If the chosen role already exists, reuse its supplied credentials or choose a new role rather than resetting its password. For a new role, use the bundled helper from the repo root:

```sh
python3 .agents/skills/setup-project/scripts/create_env.py \
  --repo-root . --db-user app_user --db-name app_dev --port 5432
```

Replace the example names and port to match the chosen local database. The helper creates a private four-line file with a random password, refuses to overwrite an existing file, and never prints credentials. If Python 3 is unavailable, create the same format with an available cryptographic random generator and restrictive file permissions. The required keys are `DB_USER`, `DB_PASSWORD`, `DB_NAME`, and `DB_URL_PREFIX`; the JDBC prefix must end with `/` because the build appends the database name.

## Provision the database

`bin/setup_database` assumes a local administrator named `postgres`, uses `psql` from PATH, and contains Bash syntax despite its `sh` shebang. It does not honor a custom host or port in `DB_URL_PREFIX`.

- For the helper's default local connection, verify admin access first and run `bash ./bin/setup_database`. Because it sources `.env` and interpolates SQL, use it only with verified plain values such as the names and hex password produced by the bundled helper.
- For a different local admin account or endpoint, use that connection to create only the missing role/database. Quote SQL identifiers and values correctly. Make a new application database owned by its application role; ensure an existing database grants that role the schema permissions Flyway needs. Do not change cluster authentication or create a superuser just to satisfy the helper.
- Verify that the configured application credentials connect to the selected database before booting. Pass passwords through protected files or process-local input/environment, keeping them out of command text and displayed output.

## Install, launch, and verify

Run these sequentially from the repo root:

```sh
./gradlew npm_ci
./gradlew bootRun
```

The explicit clean install ensures frontend tooling exists before bundling. Respect the checked-in lockfile; setup is not an opportunity to upgrade dependencies. On an already configured checkout, reinstall only if needed.

`bootRun` generates EntKt entities, DGS types, routes, and bundle entries; builds the frontend; applies Flyway migrations; and starts the server. Relay artifacts are committed, so a fresh clone does not need a separate Relay compile. Never hand-edit generated output.

- Keep the long-running process observable. Wait for successful startup or an actionable error. If the intended port is occupied, reuse the app when appropriate or choose another port with `--args='--server.port=18080'`; do not kill an unrelated process.
- Verify `/` and `/graphiql` in a browser when available. The home page loads data through Relay; seeing its actual welcome message verifies more than an HTTP 200. Execute a read-only query from the checked-in schema in GraphiQL to check the API, or use an HTTP request with the app's existing CSRF cookie/header flow. Preserve security settings.
- If Docker is running, run `./gradlew test`. If it is unavailable, report that integration tests were not run; this does not prevent verifying the app against local PostgreSQL.
- Finish with the working URL, configuration files created, checks passed or skipped, and how to stop the process you started. Leave the requested development app running. If blocked, report the specific missing prerequisite or error and the remaining step; do not claim setup succeeded.

For subsequent work: `./gradlew watchFrontend` rebuilds and typechecks in a second terminal; refresh the browser. Run `./gradlew buildRelay` after changing GraphQL queries or schema.

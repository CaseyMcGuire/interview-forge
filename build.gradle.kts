import com.github.gradle.node.npm.task.NpmTask
import org.springframework.boot.gradle.tasks.run.BootRun
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val springVersion = "4.1.1"
val dgsVersion = "12.0.1"
val javaVersion = 26
val postgresVersion = "42.7.13"
val flywayVersion = "13.5.0" // Matched to the plugin version in target file
val entktVersion = providers.gradleProperty("entktVersion").get()
val myNodeVersion = "26.8.2"
val myNpmVersion = "12.0.2"
val kotlinxHtmlVersion = "0.12.0"
// Matches the version managed by the spring-boot-dependencies BOM. It has to be stated explicitly because
// that BOM provides Testcontainers through a nested testcontainers-bom import, which the
// io.spring.dependency-management plugin doesn't surface to the org.testcontainers:* coordinates.
val testcontainersVersion = "2.0.5"

val pathToApplicationFolder = "src/main/kotlin/com"
val applicationFolder = File(rootProject.projectDir, pathToApplicationFolder)
val applicationFolderName = applicationFolder.list()?.singleOrNull()
  ?: throw IllegalStateException(
    "This application assumes that the path to the application code is $pathToApplicationFolder " +
        "with a single folder. However, it instead found the following files: ${applicationFolder.list()}. This assumption is " +
        "needed because some libraries use codegen and we need to keep the paths we pass to these libraries to be consistent " +
        "with the project structure."
  )

val dgsCodegenPackage = "com.${applicationFolderName}.graphql"

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.4.20"
  // Kotlin makes all classes final by default but Spring relies
  // upon classes being extendable to implement certain functionality.
  id("org.jetbrains.kotlin.plugin.spring") version "2.4.20"
  id("org.springframework.boot") version "4.1.1"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.github.node-gradle.node") version "7.1.0"
  id("com.netflix.dgs.codegen") version "8.6.0"
  id("io.entkt")
  id("org.flywaydb.flyway") version "13.5.0"
  id("io.github.caseymcguire.spa-routing") version "0.3.0"
  id("java")
}

dependencyManagement {
  imports {
    mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:${dgsVersion}")
    mavenBom("org.springframework.boot:spring-boot-dependencies:${springVersion}")
  }
  dependencies {
    // DGS 12 needs json-path 3.0.0 for its Jackson 3 integration (com.jayway.jsonpath.spi.json.Jackson3JsonProvider).
    // The Spring Boot 4 BOM is imported last and otherwise wins, forcing the older 2.10.0 (even over DGS's
    // strict 3.0.0 requirement); 2.10.0 lacks that class, so the DGS query executor fails to instantiate at
    // startup. Pin 3.0.0 explicitly for every configuration.
    dependency("com.jayway.jsonpath:json-path:3.0.0")
  }
}

group = "com.application"
version = "1.0-SNAPSHOT"

springBoot {
  mainClass.set("com.application.MainKt")
}

repositories {
  mavenCentral()
  // Also allow locally published spa-routing artifacts
  mavenLocal()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.springframework.boot:spring-boot-starter-security")

  implementation("com.netflix.graphql.dgs:dgs-starter")

  // Schemas compile independently so EntKt can generate the app's entity client before compileKotlin.
  schemas(project(":ent-schema"))
  entktCodegen("io.entkt:codegen:$entktVersion")
  entktCodegen("io.entkt:postgres:$entktVersion")
  implementation("io.entkt:runtime:$entktVersion")
  implementation("io.entkt:postgres:$entktVersion")

  implementation("org.postgresql:postgresql:${postgresVersion}")

  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
  implementation("tools.jackson.module:jackson-module-kotlin")

  // spring-boot-starter-jdbc supplies the DataSource/HikariCP autoconfiguration (consumed by
  // DatabaseConfiguration and by Flyway). We deliberately avoid spring-boot-starter-data-jpa: this app
  // does its data access with EntKt, so Hibernate/Spring Data JPA would be dead weight.
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  // Flyway applies its migrations on startup. Spring Boot 4 split each integration's autoconfiguration
  // into its own module, so the Flyway autoconfiguration (formerly bundled in spring-boot-autoconfigure)
  // must be pulled in explicitly via spring-boot-flyway, alongside the engine and the Postgres support.
  implementation("org.springframework.boot:spring-boot-flyway")
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

  // spa-routing: shared SPA route definitions (single source of truth) + the Spring Boot
  // starter that serves them. The starter pulls in spa-routing-core and the autoconfigure.
  implementation(project(":spa-route-definitions"))
  implementation("io.github.caseymcguire:spa-routing-spring-boot-starter:0.3.0")

  // Testing. The spring-boot-* artifacts are versioned by the spring-boot-dependencies BOM; the
  // org.testcontainers:* modules are pinned to $testcontainersVersion (see the note by its declaration).
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
  testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainersVersion")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain(javaVersion)
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
  }
}

// Code generation loads the compiled route definitions in the Gradle daemon.
// Keep its JVM aligned with the application's bytecode version.
tasks.updateDaemonJvm {
  languageVersion.set(JavaLanguageVersion.of(javaVersion))
  toolchainDownloadUrls.empty()
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(javaVersion)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

entkt {
  packageName.set("com.${applicationFolderName}.ent")
}

// Generate SPA routes from the single source of truth in :spa-route-definitions:
//  - generateClientRoutes    -> typed TS route builders under src/main/web-frontend/routes
//  - generateBundleEntries   -> overwrites SinglePageApplicationBundles.ts (Vite input map)
//  - generateServerSpaRoutes -> typed Kotlin route objects (auto-wired into compileKotlin)
spaRouting {
  routeDefinitions {
    projectPath = ":spa-route-definitions"
    sourceDirectory = "src/main/kotlin/com/application/spa"
  }
  clientRoutes {
    // Deliberately outside __generated__: the Relay compiler cleans unexpected files from its
    // artifact directory, which would delete these generated route builders on every run.
    outputDirectory = "src/main/web-frontend/routes"
  }
  serverRoutes {
    packageName = "com.application.generated.spa.routes"
    sourceRoot = "build/generated/source/spaRoutes/main"
  }
  bundleEntries {
    outputFile = "SinglePageApplicationBundles.ts"
  }
}

tasks.register<NpmTask>("buildFrontend") {
  // Generate the bundle-entry file and typed client routes before bundling/typechecking
  dependsOn("generateBundleEntries", "generateClientRoutes")
  npmCommand.set(listOf("run", "build"))
}

tasks.register<NpmTask>("watchFrontend") {
  dependsOn("generateBundleEntries", "generateClientRoutes")
  npmCommand.set(listOf("run", "watch"))
}

tasks.register<NpmTask>("buildRelay") {
  npmCommand.set(listOf("run", "relay-compiler"))
}

// make sure the frontend bundle runs before the processResources task so the TypeScript files
// are compiled before being copied into the build folder
tasks.processResources {
  val taskNames = gradle.startParameter.taskNames
  // only run frontend tasks when we're doing a full build
  if (taskNames.any { it.contains("bootRun", ignoreCase = true) }) {
    dependsOn("npm_install", "buildFrontend")
  }
}

// Note the node and npm variables can't be named `nodeVersion` and `npmVersion` since it interferes with
// the plugin
node {
  version.set(myNodeVersion)
  npmVersion.set(myNpmVersion)
  download.set(true)
  // The plugin derives the Node download's architecture from the JVM's os.arch, so a
  // Gradle daemon on an x86_64 JDK running under Rosetta (e.g. an Intel-build JDK 21 on
  // Apple silicon) downloads x64 Node. That node's npm then prunes the arm64 native
  // bindings (rolldown, esbuild) from node_modules that every arm64 invocation needs —
  // and vice versa — producing "Cannot find native binding" errors that alternate
  // between environments. Pin the platform to the real hardware instead; uname can't be
  // used because Rosetta translates child processes too, but sysctl reports the truth.
  if (System.getProperty("os.name").lowercase().contains("mac")) {
    val isAppleSilicon = runCatching {
      ProcessBuilder("sysctl", "-n", "hw.optional.arm64").start()
        .inputStream.bufferedReader().readText().trim() == "1"
    }.getOrDefault(false)
    if (isAppleSilicon) {
      resolvedPlatform.set(com.github.gradle.node.util.Platform("darwin", "arm64"))
    }
  }
}

tasks.withType<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask> {
  schemaPaths = mutableListOf("${projectDir}/src/main/resources/schema")
  generateClient = true
  packageName = dgsCodegenPackage
}

val getEnvironmentVariables = fun(): Map<String, String> {
  val map = hashMapOf<String, String>()

  val envFile = file(".env")
  if (!envFile.exists()) {
    return map
  }
  envFile.readLines().forEach {
    val (key, value) = it.split("=")
    map[key] = value
  }
  return map
}
val envVariables: Map<String, String> = getEnvironmentVariables()

val dbUser = envVariables.getValue("DB_USER")
val dbPassword = envVariables.getValue("DB_PASSWORD")
val dbUrl = envVariables.getValue("DB_URL_PREFIX") + envVariables.getValue("DB_NAME")

tasks.getByName<BootRun>("bootRun") {
  // This makes the environment variables specified in the .env file accessible to the application
  environment = envVariables
  mainClass.set("com.application.MainKt")
}

// Spring automatically handles flyway migrations but adding this task allows running flyway tasks
// from the command line. See: https://flywaydb.org/documentation/usage/gradle/
flyway {
  url = dbUrl
  user = dbUser
  password = dbPassword
}

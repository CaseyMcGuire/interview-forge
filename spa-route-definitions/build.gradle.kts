// App-owned module holding the single source of truth for SPA routes: the concrete
// SpaApplicationDefinition objects. The spa-routing Gradle plugin (applied on the root
// project) loads these at codegen time to generate the TypeScript route builders, the
// bundle-entry file consumed by vite.config.ts, and the typed Kotlin server route
// objects. It lives in its own module so the plugin's generateServerSpaRoutes ->
// :spa-route-definitions:classes dependency does not form a cycle with the root
// project's compileKotlin.
plugins {
  kotlin("jvm")
}

// Match the root project's Java 26 toolchain so the produced bytecode is consumable by it
kotlin {
  jvmToolchain(26)
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  // `api` so the root app transitively gets the route contract types
  // (SpaApplicationDefinition, SpaTypedRoute, SpaRouteTarget) used by generated code.
  api("io.github.caseymcguire:spa-routing-core:0.3.0")
}

// App-owned module holding the single source of truth for SPA routes: the concrete
// SinglePageApplicationDefinition objects. The spa-routing Gradle plugin (applied on the root
// project) loads these at codegen time to generate the TypeScript route builders, the
// bundle-entry file consumed by vite.config.ts, and the typed Kotlin server route
// objects and application configs. It lives in its own module so generateServerRoutes ->
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
  implementation("io.github.caseymcguire:spa-routing-core:0.5.1")
}

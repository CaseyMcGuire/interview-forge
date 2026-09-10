pluginManagement {
  repositories {
    // EntKt and spa-routing Gradle plugins are published to mavenLocal
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("io.entkt") version providers.gradleProperty("entktVersion").get()
  }
}

rootProject.name = "application"

// Single source of truth for SPA route definitions (see AGENTS.md)
include("spa-route-definitions")

// Entity definitions must compile before the root project's generated entity sources.
include("ent-schema")

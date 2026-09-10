plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(26)
}

repositories {
  mavenCentral()
  mavenLocal()
}

val entktVersion = providers.gradleProperty("entktVersion").get()

dependencies {
  implementation("io.entkt:schema:$entktVersion")
  // JsonElement test payloads use the same serialization version as the pinned EntKt runtime.
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

plugins {
  kotlin("jvm")
  application
}

repositories {
  mavenCentral()
}

dependencies {
  // Use the same JSON libraries and versions as the application, without Spring or database code.
  implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
  implementation("tools.jackson.core:jackson-databind")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
}

kotlin {
  jvmToolchain(21)
}

application {
  mainClass.set("com.application.execution.KotlinTestSuite")
}

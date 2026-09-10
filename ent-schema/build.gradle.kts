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
}

plugins {
  kotlin("jvm") version "2.1.21" // Match your project Kotlin version
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.squareup:kotlinpoet:2.2.0")
  implementation(kotlin("reflect"))
}

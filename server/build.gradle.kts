import jdk.tools.jlink.resources.plugins

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.ktor)
  alias(libs.plugins.kotlinx.serialization)
  application
}

group = "com.aryamahasangh"
version = "1.0.0"
application {
  mainClass.set("com.aryamahasangh.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    //implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation("com.expediagroup:graphql-kotlin-ktor-server:8.3.0")
    implementation(libs.ktor.server.statuspages)
    implementation(libs.ktor.server.websockets)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-server-config-yaml:3.0.2")
}

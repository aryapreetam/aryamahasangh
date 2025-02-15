plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "org.aryamahasangh"
version = "1.0.0"
application {
    mainClass.set("org.aryamahasangh.ApplicationKt")
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
    implementation(libs.ktor.server.sse)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
}

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
    implementation("com.expediagroup:graphql-kotlin-ktor-server:8.3.0")

    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}

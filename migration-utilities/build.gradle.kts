plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  application
}

dependencies {
  implementation("io.github.jan-tennert.supabase:supabase-kt:2.0.0")
  implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")
  implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
  implementation("io.ktor:ktor-client-cio:2.3.7")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}

application {
  mainClass.set("com.aryamahasangh.migration.MainKt")
}

// Create tasks for each utility
tasks.register<JavaExec>("preFlightCheck") {
  group = "migration"
  description = "Run pre-flight validation"
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("com.aryamahasangh.migration.PreFlightCheckKt")
}

tasks.register<JavaExec>("environmentDiff") {
  group = "migration"
  description = "Compare environments"
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("com.aryamahasangh.migration.EnvironmentDiffKt")
}

tasks.register<JavaExec>("isolationAuditor") {
  group = "migration"
  description = "Audit environment isolation"
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("com.aryamahasangh.migration.IsolationAuditorKt")
}

tasks.register<JavaExec>("progressMonitor") {
  group = "migration"
  description = "Monitor migration progress"
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("com.aryamahasangh.migration.ProgressMonitorKt")
}

tasks.register<JavaExec>("autoConfigureGraphql") {
  group = "migration"
  description = "Auto-configure GraphQL"
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("com.aryamahasangh.migration.AutoConfigureGraphqlKt")
}

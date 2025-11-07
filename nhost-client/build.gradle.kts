@file:OptIn(ApolloExperimental::class)

import com.apollographql.apollo.annotations.ApolloExperimental
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.util.*

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.apollo)
  alias(libs.plugins.kmp.secrets.plugin)
}

// Load secrets from local.properties for build-time operations
fun loadLocalProperties(): Properties {
  val localPropsFile = rootProject.file("local.properties")
  val props = Properties()

  if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { props.load(it) }
  }

  return props
}

val localProps = loadLocalProperties()

// Get environment from project properties (can be passed via -Penv=dev/staging/prod)
val environment = project.findProperty("env")?.toString() ?: localProps.getProperty("environment", "dev")

secretsConfig {
  outputDir = layout.buildDirectory
    .dir("generated/kmp-secrets/commonMain/kotlin")
    .get()
    .asFile
    .absolutePath
}

// Disable secrets generation for all *Test compilations to avoid shared-output conflicts
tasks.configureEach {
  if (name.startsWith("generateSecrets") && name.contains("Test")) {
    enabled = false
  }
}

kotlin {
  jvmToolchain(17)

  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  jvm("desktop")

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
  }

  sourceSets {
    val commonMain by getting {
      kotlin.srcDir(
        layout.buildDirectory
          .dir("generated/kmp-secrets/commonMain/kotlin")
          .get()
          .asFile
          .absolutePath
      )
      dependencies {
        // apollo runtime
        implementation(libs.apollo.runtime)
        // apollo adapters
        implementation(libs.apollo.adapters.core)
        implementation(libs.apollo.adapters.kotlinx.datetime)

        // apollo normalized cache
        implementation(libs.apollo.normalized.cache)

        // Kotlin serialization
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)

        // Ktor for networking
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.ktor.client.android)
      }
    }

    val iosMain by creating {
      dependsOn(commonMain)
      dependencies {
        implementation(libs.ktor.client.darwin)
      }
    }

    val desktopMain by getting {
      dependencies {
        implementation(libs.ktor.client.java)
      }
    }

    val wasmJsMain by getting {
      dependencies {
        implementation(libs.ktor.client.wasm)
      }
    }

    // Configure iOS source sets
    val iosX64Main by getting
    val iosArm64Main by getting
    val iosSimulatorArm64Main by getting

    iosX64Main.dependsOn(iosMain)
    iosArm64Main.dependsOn(iosMain)
    iosSimulatorArm64Main.dependsOn(iosMain)
  }
}

android {
  namespace = "com.aryamahasangh.nhost"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

// Apollo configuration for Nhost GraphQL
apollo {
  service("nhost") {
    packageName.set("com.aryamahasangh.nhost")

    // Map scalar types
    mapScalar("timestamptz", "kotlinx.datetime.Instant")
    mapScalar("date", "kotlinx.datetime.LocalDate")
    mapScalar("uuid", "kotlin.String")
    mapScalar("smallint", "kotlin.Int")

    // Generate Kotlin models
    generateKotlinModels.set(true)
    generateInputBuilders.set(true)
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)

    // Read Nhost credentials from local.properties based on environment
    // Format: nhost_<env>_url, nhost_<env>_admin_secret
    val nhostGraphqlUrl = localProps.getProperty("nhost_${environment}_graphql_url", "https://hwdbpplmrdjdhcsmdleh.hasura.ap-south-1.nhost.run/v1/graphql")
    val nhostAdminSecret = localProps.getProperty("nhost_${environment}_admin_secret", "")

    introspection {
      endpointUrl.set(nhostGraphqlUrl)
      schemaFile.set(file("src/commonMain/graphql/schema.graphqls"))
      if (nhostAdminSecret.isNotEmpty()) {
        headers.put("x-hasura-admin-secret", nhostAdminSecret)
      }
    }
  }
}

// Ensure ALL Kotlin compile tasks that read the shared secrets output run AFTER the secrets generators
tasks.withType(AbstractKotlinCompileTool::class.java).configureEach {
  dependsOn(tasks.matching { it.name.startsWith("generateSecrets") && !it.name.contains("Test") })
}

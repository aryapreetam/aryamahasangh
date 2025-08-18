package com.aryamahasangh.config

import secrets.Secrets

/**
 * Application configuration using the generated Secrets object from KMP-Secrets-Plugin.
 * All secrets are loaded from local.properties and converted to type-safe constants.
 */
object AppConfig {

  val environment: String
    get() = Secrets.environment

  // Version information derived from single app_version property
  val versionName: String
    get() = Secrets.app_version

  val versionCode: Int
    get() = calculateVersionCode(Secrets.app_version)

  private fun calculateVersionCode(version: String): Int {
    val parts = version.split(".")
    return when (parts.size) {
      3 -> parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
      2 -> parts[0].toInt() * 100 + parts[1].toInt()
      1 -> parts[0].toInt()
      else -> 1
    }
  }

  val supabaseUrl: String
    get() = when (environment) {
      "prod" -> Secrets.prod_supabase_url
      "staging" -> Secrets.staging_supabase_url
      else -> Secrets.dev_supabase_url
    }

  val supabaseKey: String
    get() = when (environment) {
      "prod" -> Secrets.prod_supabase_key
      "staging" -> Secrets.staging_supabase_key
      else -> Secrets.dev_supabase_key
    }

  val serverUrl: String
    get() = when (environment) {
      "prod" -> Secrets.prod_server_url.ifEmpty { supabaseUrl }
      "staging" -> Secrets.staging_server_url.ifEmpty { supabaseUrl }
      else -> Secrets.dev_server_url
    }

  val graphqlUrl: String
    get() = "$serverUrl/graphql"

  val subscriptionsUrl: String
    get() = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/subscriptions"

  const val STORAGE_BUCKET = "documents"

  val keystorePassword: String
    get() = when (environment) {
      "prod" -> Secrets.prod_keystore_password
      "staging" -> Secrets.staging_keystore_password
      else -> Secrets.dev_keystore_password
    }

  val keyAlias: String
    get() = when (environment) {
      "prod" -> Secrets.prod_key_alias
      "staging" -> Secrets.staging_key_alias
      else -> Secrets.dev_key_alias
    }

  val keyPassword: String
    get() = when (environment) {
      "prod" -> Secrets.prod_key_password
      "staging" -> Secrets.staging_key_password
      else -> Secrets.dev_key_password
    }

  fun getConfigInfo(): String =
    """
    Version: $versionName ($versionCode)
    Environment: $environment
    Supabase URL: ${supabaseUrl.take(20)}...
    Server URL: $serverUrl
    GraphQL URL: $graphqlUrl
    Subscriptions URL: $subscriptionsUrl
    """.trimIndent()
}

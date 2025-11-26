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

  val sentryDsn: String
    get() = Secrets.sentry

  val supabaseKey: String
    get() = when (environment) {
      "prod" -> Secrets.prod_supabase_key
      "staging" -> Secrets.staging_supabase_key
      else -> Secrets.dev_supabase_key
    }

//  val subscriptionsUrl: String
//    get() = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/subscriptions"

  const val STORAGE_BUCKET = "documents"

  fun getConfigInfo(): String =
    """
    Version: $versionName ($versionCode)
    Environment: $environment
    Supabase URL: ${supabaseUrl.take(20)}...
    """.trimIndent()
}

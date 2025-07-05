package com.aryamahasangh.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppConfigTest {

  @Test
  fun testEnvironmentIsLoaded() {
    assertEquals("dev", AppConfig.environment)
  }

  @Test
  fun testSupabaseUrlIsLoadedForDevEnvironment() {
    assertTrue(AppConfig.supabaseUrl.isNotEmpty())
    assertTrue(AppConfig.supabaseUrl.startsWith("https://"))
  }

  @Test
  fun testSupabaseKeyIsLoadedForDevEnvironment() {
    assertTrue(AppConfig.supabaseKey.isNotEmpty())
  }

  @Test
  fun testServerUrlIsLoadedForDevEnvironment() {
    assertTrue(AppConfig.serverUrl.isNotEmpty())
  }

  @Test
  fun testGraphqlUrlIsGenerated() {
    assertTrue(AppConfig.graphqlUrl.endsWith("/graphql"))
  }

  @Test
  fun testConfigInfoIsGenerated() {
    val configInfo = AppConfig.getConfigInfo()
    assertTrue(configInfo.contains("Environment: dev"))
    assertTrue(configInfo.contains("Supabase URL:"))
  }
}

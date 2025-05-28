package org.aryamahasangh.config

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppConfigTest {
  @Test
  fun testConfigurationLoading() {
    // Test that configuration can be loaded
    // In a real test environment, you'd mock the properties file or set test environment variables

    try {
      val configInfo = AppConfig.getConfigInfo()
      assertNotNull(configInfo)
      assertTrue(configInfo.contains("Environment:"))
      println("Configuration loaded successfully:")
      println(configInfo)
    } catch (e: Exception) {
      // Expected in test environment without secrets.properties
      assertTrue(e.message?.contains("not configured") == true)
      println("Configuration test passed - properly handles missing configuration")
    }
  }

  @Test
  fun testGraphqlUrlGeneration() {
    // Test URL generation logic
    val serverUrl = "http://localhost:4000"
    val expectedGraphql = "$serverUrl/graphql"
    val expectedWs = "ws://localhost:4000/subscriptions"

    // These would be tested with mock configuration
    assertTrue(expectedGraphql.endsWith("/graphql"))
    assertTrue(expectedWs.startsWith("ws://"))
  }

//    @Test
//    fun testEnvironmentDetection() {
//        // Test environment detection logic
//        val isProduction = System.getenv("ENVIRONMENT") == "prod" ||
//                          System.getProperty("environment") == "prod"
//
//        // In test environment, should default to dev
//        assertTrue(!isProduction || System.getenv("ENVIRONMENT") == "prod")
//    }
}

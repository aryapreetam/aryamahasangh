package org.aryamahasangh.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecretsLoaderTest {
    
    @Test
    fun testParseProperties() {
        val propertiesContent = """
            # This is a comment
            environment=dev
            dev.supabase.url=https://test.supabase.co
            dev.supabase.key=test-key
            
            # Another comment
            prod.server.url=https://prod-server.com
        """.trimIndent()
        
        val result = SecretsUtils.parseProperties(propertiesContent)
        
        assertEquals("dev", result["environment"])
        assertEquals("https://test.supabase.co", result["dev.supabase.url"])
        assertEquals("test-key", result["dev.supabase.key"])
        assertEquals("https://prod-server.com", result["prod.server.url"])
        
        // Comments should be ignored
        assertTrue(result.keys.none { it.startsWith("#") })
    }
    
    @Test
    fun testConfigInitializer() {
        // Reset state
        ConfigInitializer.reset()
        
        // Should not be initialized initially
        assertEquals(false, ConfigInitializer.isInitialized())
        
        // Initialize
        ConfigInitializer.initialize()
        
        // Should be initialized now
        assertEquals(true, ConfigInitializer.isInitialized())
        
        // Should be able to access config
        assertNotNull(AppConfig.current)
    }
    
    @Test
    fun testAppConfigInitialization() {
        val testConfig = mapOf(
            "environment" to "test",
            "test.supabase.url" to "https://test.supabase.co",
            "test.supabase.key" to "test-key",
            "test.server.url" to "http://localhost:4000"
        )
        
        AppConfig.initialize(testConfig)
        
        assertEquals("test", AppConfig.environment)
        assertEquals("https://test.supabase.co", AppConfig.supabaseUrl)
        assertEquals("test-key", AppConfig.supabaseKey)
        assertEquals("http://localhost:4000", AppConfig.serverUrl)
        assertEquals("http://localhost:4000/graphql", AppConfig.graphqlUrl)
        assertEquals("ws://localhost:4000/subscriptions", AppConfig.subscriptionsUrl)
    }
}
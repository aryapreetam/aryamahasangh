package org.aryamahasangh.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cache entry with expiration time
 */
@Serializable
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long,
    val expirationTime: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime
}

/**
 * In-memory cache manager with TTL support
 */
class CacheManager {
    private val cache = mutableMapOf<String, String>()
    private val mutex = Mutex()
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Store data in cache with TTL
     */
    suspend inline fun <reified T> put(
        key: String, 
        data: T, 
        ttlMs: Long = DEFAULT_TTL_MS
    ) {
        mutex.withLock {
            val entry = CacheEntry(
                data = data,
                timestamp = System.currentTimeMillis(),
                expirationTime = System.currentTimeMillis() + ttlMs
            )
            cache[key] = json.encodeToString(entry)
        }
    }
    
    /**
     * Retrieve data from cache
     */
    suspend inline fun <reified T> get(key: String): T? {
        return mutex.withLock {
            val entryJson = cache[key] ?: return@withLock null
            
            try {
                val entry = json.decodeFromString<CacheEntry<T>>(entryJson)
                
                if (entry.isExpired()) {
                    cache.remove(key)
                    null
                } else {
                    entry.data
                }
            } catch (e: Exception) {
                // Remove corrupted cache entry
                cache.remove(key)
                null
            }
        }
    }
    
    /**
     * Check if key exists and is not expired
     */
    suspend fun contains(key: String): Boolean {
        return mutex.withLock {
            val entryJson = cache[key] ?: return@withLock false
            
            try {
                val entry = json.decodeFromString<CacheEntry<Any>>(entryJson)
                if (entry.isExpired()) {
                    cache.remove(key)
                    false
                } else {
                    true
                }
            } catch (e: Exception) {
                cache.remove(key)
                false
            }
        }
    }
    
    /**
     * Remove specific key from cache
     */
    suspend fun remove(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }
    
    /**
     * Clear all cache entries
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    /**
     * Remove expired entries
     */
    suspend fun cleanupExpired() {
        mutex.withLock {
            val keysToRemove = mutableListOf<String>()
            
            cache.forEach { (key, entryJson) ->
                try {
                    val entry = json.decodeFromString<CacheEntry<Any>>(entryJson)
                    if (entry.isExpired()) {
                        keysToRemove.add(key)
                    }
                } catch (e: Exception) {
                    keysToRemove.add(key)
                }
            }
            
            keysToRemove.forEach { cache.remove(it) }
        }
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getStats(): CacheStats {
        return mutex.withLock {
            var expiredCount = 0
            var validCount = 0
            
            cache.values.forEach { entryJson ->
                try {
                    val entry = json.decodeFromString<CacheEntry<Any>>(entryJson)
                    if (entry.isExpired()) {
                        expiredCount++
                    } else {
                        validCount++
                    }
                } catch (e: Exception) {
                    expiredCount++
                }
            }
            
            CacheStats(
                totalEntries = cache.size,
                validEntries = validCount,
                expiredEntries = expiredCount
            )
        }
    }
    
    companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L // 5 minutes
        const val LONG_TTL_MS = 60 * 60 * 1000L // 1 hour
        const val SHORT_TTL_MS = 60 * 1000L // 1 minute
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int
)

/**
 * Cache keys for different data types
 */
object CacheKeys {
    const val ACTIVITIES = "activities"
    const val ACTIVITY_DETAIL = "activity_detail_"
    const val STUDENT_APPLICATIONS = "student_applications"
    const val ORGANISATIONS_AND_MEMBERS = "organisations_and_members"
    const val ABOUT_US = "about_us"
    const val LEARNING_CONTENT = "learning_content"
    const val BOOK_ORDERS = "book_orders"
    const val JOIN_US_CONTENT = "join_us_content"
    const val ORGANISATIONS = "organisations"
}
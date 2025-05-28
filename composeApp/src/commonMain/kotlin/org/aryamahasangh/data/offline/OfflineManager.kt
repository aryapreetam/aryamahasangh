package org.aryamahasangh.data.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.aryamahasangh.data.cache.CacheManager
import org.aryamahasangh.util.Result
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Represents an offline operation that needs to be synced
 */
@Serializable
data class OfflineOperation(
  val id: String,
  val type: OperationType,
  val data: String,
  val timestamp: Long,
  val retryCount: Int = 0
)

/**
 * Types of operations that can be performed offline
 */
@Serializable
enum class OperationType {
  CREATE_ACTIVITY,
  DELETE_ACTIVITY,
  SUBMIT_ADMISSION_FORM,
  SUBMIT_JOIN_US_FORM,
  CREATE_BOOK_ORDER
}

/**
 * Network connectivity state
 */
enum class NetworkState {
  CONNECTED,
  DISCONNECTED,
  UNKNOWN
}

/**
 * Manages offline operations and data synchronization
 */
class OfflineManager(
  val cacheManager: CacheManager
) {
  private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
  val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

  val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val pendingOperations = mutableListOf<OfflineOperation>()
  private val mutex = Mutex()
  private val json =
    Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

  /**
   * Update network connectivity state
   */
  fun updateNetworkState(isConnected: Boolean) {
    _networkState.value = if (isConnected) NetworkState.CONNECTED else NetworkState.DISCONNECTED
    _isOnline.value = isConnected

    if (isConnected) {
      // Trigger sync when coming back online
      // In a real app, you would call syncPendingOperations() here
    }
  }

  /**
   * Queue an operation for offline execution
   */
  @OptIn(ExperimentalTime::class)
  suspend fun queueOperation(
    type: OperationType,
    data: Any,
    operationId: String = generateOperationId()
  ) {
    mutex.withLock {
      val operation =
        OfflineOperation(
          id = operationId,
          type = type,
          data = json.encodeToString(data),
          timestamp = Clock.System.now().epochSeconds
        )
      pendingOperations.add(operation)

      // Persist to cache for durability
      cacheManager.put(
        key = "offline_operations",
        data = pendingOperations,
        ttlMs = CacheManager.LONG_TTL_MS * 24 // 24 hours
      )
    }
  }

  /**
   * Get all pending operations
   */
  suspend fun getPendingOperations(): List<OfflineOperation> {
    return mutex.withLock {
      pendingOperations.toList()
    }
  }

  /**
   * Remove a completed operation
   */
  suspend fun removeOperation(operationId: String) {
    mutex.withLock {
      pendingOperations.removeAll { it.id == operationId }

      // Update cache
      cacheManager.put(
        key = "offline_operations",
        data = pendingOperations,
        ttlMs = CacheManager.LONG_TTL_MS * 24
      )
    }
  }

  /**
   * Increment retry count for a failed operation
   */
  suspend fun incrementRetryCount(operationId: String) {
    mutex.withLock {
      val index = pendingOperations.indexOfFirst { it.id == operationId }
      if (index != -1) {
        val operation = pendingOperations[index]
        pendingOperations[index] = operation.copy(retryCount = operation.retryCount + 1)

        // Update cache
        cacheManager.put(
          key = "offline_operations",
          data = pendingOperations,
          ttlMs = CacheManager.LONG_TTL_MS * 24
        )
      }
    }
  }

  /**
   * Load pending operations from cache
   */
  suspend fun loadPendingOperations() {
    mutex.withLock {
      val cachedOperations = cacheManager.get<List<OfflineOperation>>("offline_operations")
      if (cachedOperations != null) {
        pendingOperations.clear()
        pendingOperations.addAll(cachedOperations)
      }
    }
  }

  /**
   * Clear all pending operations
   */
  suspend fun clearPendingOperations() {
    mutex.withLock {
      pendingOperations.clear()
      cacheManager.remove("offline_operations")
    }
  }

  /**
   * Get cached data with fallback strategy
   */
  suspend inline fun <reified T> getCachedDataWithFallback(
    key: String,
    onlineDataProvider: suspend () -> Result<T>
  ): Result<T> {
    return if (_isOnline.value) {
      // Try to get fresh data when online
      when (val result = onlineDataProvider()) {
        is Result.Success -> {
          // Cache the fresh data
          cacheManager.put(key, result.data)
          result
        }
        is Result.Error -> {
          // Fallback to cached data if online request fails
          val cachedData = cacheManager.get<T>(key)
          if (cachedData != null) {
            Result.Success(cachedData)
          } else {
            result
          }
        }
        is Result.Loading -> result
      }
    } else {
      // Use cached data when offline
      val cachedData = cacheManager.get<T>(key)
      if (cachedData != null) {
        Result.Success(cachedData)
      } else {
        Result.Error("No cached data available and device is offline")
      }
    }
  }

  /**
   * Execute operation with offline support
   */
  suspend inline fun <reified T> executeWithOfflineSupport(
    operation: suspend () -> Result<T>,
    offlineData: T? = null,
    cacheKey: String? = null
  ): Result<T> {
    return if (_isOnline.value) {
      val result = operation()

      // Cache successful results
      if (result is Result.Success && cacheKey != null) {
        cacheManager.put(cacheKey, result.data)
      }

      result
    } else {
      // Return offline data or cached data
      when {
        offlineData != null -> Result.Success(offlineData)
        cacheKey != null -> {
          val cachedData = cacheManager.get<T>(cacheKey)
          if (cachedData != null) {
            Result.Success(cachedData)
          } else {
            Result.Error("No offline data available")
          }
        }
        else -> Result.Error("Device is offline and no fallback data available")
      }
    }
  }

  @OptIn(ExperimentalTime::class)
  private fun generateOperationId(): String {
    return "op_${Clock.System.now().epochSeconds}_${(0..999).random()}"
  }

  companion object {
    const val MAX_RETRY_COUNT = 3
    const val RETRY_DELAY_MS = 5000L
  }
}

/**
 * Extension function to check if device is online
 */
fun OfflineManager.requiresOnline(): Boolean = !isOnline.value

/**
 * Extension function to execute operation only when online
 */
suspend fun <T> OfflineManager.executeOnlineOnly(
  operation: suspend () -> Result<T>
): Result<T> {
  return if (isOnline.value) {
    operation()
  } else {
    Result.Error("This operation requires an internet connection")
  }
}

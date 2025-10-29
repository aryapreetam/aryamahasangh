package com.aryamahasangh.nhost.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of SecureStorage for testing and fallback
 * Note: This is NOT secure and should only be used for development/testing
 */
class InMemorySecureStorage : SecureStorage {
  private val storage = mutableMapOf<String, String>()
  private val mutex = Mutex()

  override suspend fun saveString(key: String, value: String) {
    mutex.withLock {
      storage[key] = value
    }
  }

  override suspend fun getString(key: String): String? {
    return mutex.withLock {
      storage[key]
    }
  }

  override suspend fun remove(key: String) {
    mutex.withLock {
      storage.remove(key)
    }
  }

  override suspend fun clear() {
    mutex.withLock {
      storage.clear()
    }
  }
}


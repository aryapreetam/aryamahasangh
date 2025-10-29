package com.aryamahasangh.nhost.storage

/**
 * Interface for secure storage of sensitive data
 * Platform-specific implementations handle encryption
 */
interface SecureStorage {
  suspend fun saveString(key: String, value: String)
  suspend fun getString(key: String): String?
  suspend fun remove(key: String)
  suspend fun clear()

  companion object {
    const val KEY_REFRESH_TOKEN = "nhost_refresh_token"
    const val KEY_ACCESS_TOKEN = "nhost_access_token"
    const val KEY_EXPIRES_AT = "nhost_expires_at"
    const val KEY_USER_ID = "nhost_user_id"
  }
}


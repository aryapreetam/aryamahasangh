package com.aryamahasangh.network

import com.apollographql.adapter.datetime.KotlinxInstantAdapter
import com.apollographql.adapter.datetime.KotlinxLocalDateAdapter
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.aryamahasangh.config.AppConfig
import com.aryamahasangh.type.Date
import com.aryamahasangh.type.Datetime
import com.aryamahasangh.util.Result
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.graphql.GraphQL
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.SettingsResumableCache
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json

/**
 * Supabase client configured using the unified configuration system.
 * No hard-coded secrets - all configuration loaded from AppConfig.
 */
val supabaseClient =
  createSupabaseClient(
    supabaseUrl = AppConfig.supabaseUrl,
    supabaseKey = AppConfig.supabaseKey
  ) {
    install(Storage) {
      resumable {
        cache = SettingsResumableCache()
      }
    }
    install(Postgrest)
    install(Realtime)
    install(Auth) {
      // Use PKCE flow for better security (especially important for mobile/web)
      flowType = FlowType.PKCE

      // Enable automatic session refresh
      autoLoadFromStorage = true
      alwaysAutoRefresh = true

      // Platform-specific secure storage is automatically handled by the SDK:
      // - Android: Uses EncryptedSharedPreferences
      // - iOS: Uses Keychain
      // - Web: Uses memory storage (no localStorage for security)
      // - Desktop: Uses platform-specific secure storage
    }
    install(GraphQL) {
      apolloConfiguration {
        addHttpInterceptor(httpInterceptor = ApolloHttpInterceptor())
        addInterceptor(interceptor = LoggingApolloInterceptor())
        addCustomScalarAdapter(
          customScalarType = Datetime.type,
          customScalarAdapter = KotlinxInstantAdapter
        )
        addCustomScalarAdapter(
          customScalarType = Date.type,
          customScalarAdapter = KotlinxLocalDateAdapter
        )
        normalizedCache(
          normalizedCacheFactory = MemoryCacheFactory(
            maxSizeBytes = 10 * 1024 * 1024,
            expireAfterMillis = 5 * 60 * 1000L // ✅ 5 minutes
          )
        )
      }
    }
    defaultSerializer =
      KotlinXSerializer(
        Json {
          ignoreUnknownKeys = true // Avoid errors if unknown fields are received
          encodeDefaults = true
          prettyPrint = true
        }
      )
  }

class ApolloHttpInterceptor : HttpInterceptor {
  override suspend fun intercept(
    request: HttpRequest,
    chain: HttpInterceptorChain
  ): HttpResponse {
    return chain.proceed(request)
  }
}

class LoggingApolloInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(
    request: ApolloRequest<D>,
    chain: ApolloInterceptorChain
  ): Flow<ApolloResponse<D>> {
    return chain.proceed(request).onEach {
       //println("Request: ${request.operation}")
       //println("Response ${it.data}")
    }
  }
}

val resumableClient = supabaseClient.storage[AppConfig.STORAGE_BUCKET].resumable
val bucket = supabaseClient.storage[AppConfig.STORAGE_BUCKET]

/**
 * Extension function for ApolloClient that provides cache-and-network flow with improved cache detection.
 * 
 * This function addresses the distinction between cache and network responses:
 * - `isFromCache`: True if response comes from cache, false if from network
 * 
 * The logic helps distinguish between:
 * 1. Cache response - suppress errors, show data if available, wait for network
 * 2. Network response - show errors if any, this is the final result
 */
fun <D : Query.Data, Q : Query<D>> com.apollographql.apollo.ApolloClient.resultCacheAndNetworkFlow(
    query: Q,
    extractList: (D) -> List<*>? = { null } // Optional: only needed if you're validating emptiness
): Flow<Result<D>> = flow {
    emit(Result.Loading)

    query(query)
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response: ApolloResponse<D> ->
            val isFromCache = response.isFromCache
            val data = response.data
            val list = data?.let(extractList)

            // Handle errors based on source
            if (response.hasErrors()) {
                if (isFromCache) {
                    // If from cache and has errors, suppress error and emit data if available
                    // Wait for network result
                    if (data != null) {
                        emit(Result.Success(data))
                    }
                } else {
                    // If from network and has errors, emit error
                    emit(Result.Error(response.errors?.firstOrNull()?.message ?: "Unknown error"))
                }
                return@collect
            }

            // Handle empty data based on source
            if (data == null || (list != null && list.isEmpty())) {
                if (isFromCache) {
                    // If from cache and empty, suppress error and emit empty data
                    // Wait for network result
                    if (data != null) {
                        emit(Result.Success(data))
                    }
                } else {
                    // If from network and empty, emit error
                    emit(Result.Error("No data found"))
                }
                return@collect
            }

            // Handle successful responses with data
            emit(Result.Success(data))
        }
}.catch { e ->
    emit(Result.Error(e.message ?: "Unknown error", e))
}

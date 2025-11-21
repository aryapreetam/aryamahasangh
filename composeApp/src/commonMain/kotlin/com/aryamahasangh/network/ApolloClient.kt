package com.aryamahasangh.network

import com.apollographql.adapter.datetime.KotlinxInstantAdapter
import com.apollographql.adapter.datetime.KotlinxLocalDateAdapter
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.aryamahasangh.config.AppConfig
import com.aryamahasangh.type.Date
import com.aryamahasangh.type.Datetime
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.coil.Coil3Integration
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.graphql.GraphQL
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.SettingsResumableCache
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json

/**
 * Supabase client configured using the generated Secrets object.
 * No hard-coded secrets - all configuration loaded from local.properties via KMP-Secrets-Plugin.
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
    install(Coil3Integration)
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
      if(AppConfig.environment != "prod") {
        println("Request: ${request.operation}")
        println("Response ${it.data}")
      }
    }
  }
}

val resumableClient = supabaseClient.storage[AppConfig.STORAGE_BUCKET].resumable
val bucket = supabaseClient.storage[AppConfig.STORAGE_BUCKET]


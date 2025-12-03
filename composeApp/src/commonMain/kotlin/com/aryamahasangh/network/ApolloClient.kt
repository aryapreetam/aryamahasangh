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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.coil.Coil3Integration
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.graphql.GraphQL
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json

// CRITICAL REFACTOR: Removed global variables to prevent iOS launch crash.
// SupabaseClient is now created via createAppSupabaseClient() and managed by Koin.

/**
 * Creates and configures the SupabaseClient instance.
 * This should be called ONLY by the Koin module to ensure lazy initialization.
 */
fun createAppSupabaseClient(): SupabaseClient {
  // Create SupabaseClient with provider lambda to resolve circular dependency
  lateinit var client: SupabaseClient
  
  client = createSupabaseClient(
    supabaseUrl = AppConfig.supabaseUrl,
    supabaseKey = AppConfig.supabaseKey
  ) {
    install(Auth) {
      // Use PKCE flow for better security (especially important for mobile/web)
      flowType = FlowType.PKCE

      // CRITICAL iOS FIX: Disable autoLoadFromStorage on iOS
      // The Keychain access during SupabaseClient initialization causes NSObject lifecycle crash
      // This is a known KMP limitation - Keychain isn't safe to access during early init
      // Trade-off: Users must login on each app launch on iOS
      // TODO: Implement manual session restoration after app is fully initialized
      autoLoadFromStorage = false //!isIos
      alwaysAutoRefresh = true
    }
    
    install(Storage)
    install(Coil3Integration)
    install(Postgrest)
    install(Realtime)
    
    install(GraphQL) {
      apolloConfiguration {
        addHttpInterceptor(httpInterceptor = ApolloHttpInterceptor { client })
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
            expireAfterMillis = 5 * 60 * 1000L // 5 minutes
          )
        )
      }
    }
    
    defaultSerializer = KotlinXSerializer(
      Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
      }
    )
  }
  
  return client
}

class ApolloHttpInterceptor(private val supabaseClientProvider: () -> SupabaseClient) : HttpInterceptor {
  override suspend fun intercept(
    request: HttpRequest,
    chain: HttpInterceptorChain
  ): HttpResponse {
    val supabaseClient = supabaseClientProvider()
    val accessToken = supabaseClient.auth.currentAccessTokenOrNull()
    
    val modifiedRequest = if (accessToken != null) {
      request.newBuilder()
        .addHeader("Authorization", "Bearer $accessToken")
        .build()
    } else {
      request
    }
    
    return chain.proceed(modifiedRequest)
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

// REMOVED: Global bucket variable that was causing iOS crash
// Use FileUploadUtils (injected) or SupabaseClient (injected) instead

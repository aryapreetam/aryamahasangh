package org.aryamahasangh.network

import com.apollographql.adapter.datetime.KotlinxLocalDateTimeAdapter
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.apollographql.ktor.http.KtorHttpEngine
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.SettingsResumableCache
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.aryamahasangh.config.AppConfig
import org.aryamahasangh.type.LocalDateTime

/**
 * Apollo GraphQL client configured using the unified configuration system.
 * No hard-coded secrets - all configuration loaded from AppConfig.
 */
val apolloClient = ApolloClient.Builder()
  .serverUrl(AppConfig.graphqlUrl)
  .webSocketServerUrl(AppConfig.subscriptionsUrl)
  .addHttpHeader("Access-Control-Allow-Origin", "*")
  .httpEngine(KtorHttpEngine())
  .addHttpInterceptor(AuthorizationInterceptor())
  .addCustomScalarAdapter(LocalDateTime.type, KotlinxLocalDateTimeAdapter)
  .build()

class AuthorizationInterceptor() : HttpInterceptor {
  private val mutex = Mutex()

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    var token = mutex.withLock {
      // get current token
      "Sample token"
    }

    val response = chain.proceed(request.newBuilder().addHeader("Authorization", "Bearer $token").build())

    return if (response.statusCode == 401) {
      token = mutex.withLock {
        "Sample token"
      }
      chain.proceed(request.newBuilder().addHeader("Authorization", "Bearer $token").build())
    } else {
      response
    }
  }
}


//class MyApolloInterceptor : ApolloInterceptor {
//  override fun <D : Operation.Data> intercept(
//    request: ApolloRequest<D>,
//    chain: ApolloInterceptorChain
//  ): Flow<ApolloResponse<D>> {
//    val headers = request.httpHeaders
//    val req = request.newBuilder()
//    headers?.forEach {
//      req.addHttpHeader(it.name, it.value)
//    }
//    req.addHttpHeader("custom", "jhjhj")
//    req.addHttpHeader("Access-Control-Allow-Origin", "*")
//
//    return chain.proceed(req.build())
//  }
//}

class SampleInterceptor : HttpInterceptor {
  override suspend fun intercept(
    request: HttpRequest,
    chain: HttpInterceptorChain
  ): HttpResponse {
    val requestBuilder = request.newBuilder()
    requestBuilder.addHeader("Custom", "fdfdfdf")
    return chain.proceed(requestBuilder.build())
  }
}

/**
 * Supabase client configured using the unified configuration system.
 * No hard-coded secrets - all configuration loaded from AppConfig.
 */
val supabaseClient = createSupabaseClient(
  supabaseUrl = AppConfig.supabaseUrl,
  supabaseKey = AppConfig.supabaseKey
){
  install(Storage){
    resumable {
      cache = SettingsResumableCache()
    }
  }
  install(Postgrest){
  }
  defaultSerializer = KotlinXSerializer(Json{
    ignoreUnknownKeys = true // Avoid errors if unknown fields are received
    encodeDefaults = true
    prettyPrint = true
  })
}
val resumableClient = supabaseClient.storage[AppConfig.STORAGE_BUCKET].resumable
val bucket = supabaseClient.storage[AppConfig.STORAGE_BUCKET]


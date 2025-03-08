package org.aryamahasangh.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.apollographql.ktor.http.KtorHttpEngine
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.SettingsResumableCache
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.aryamahasangh.isAndroid

const val localDev = true
// "10.0.2.2"
val host = if(isAndroid()) "192.168.250.26" else "localhost"
var serverUrl = if(localDev) "http://${host}:4000" else "https://sandhya-anushthan-api.onrender.com"
var webSocketUrl = if(localDev) "ws://${host}:4000" else "wss://sandhya-anushthan-api.onrender.com"
val apolloClient = ApolloClient.Builder()
  .serverUrl("$serverUrl/graphql")
  .webSocketServerUrl("$webSocketUrl/subscriptions")
  .addHttpHeader("Access-Control-Allow-Origin", "*")
  .httpEngine(KtorHttpEngine())
  .addHttpInterceptor(AuthorizationInterceptor())
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

val supabaseClient = createSupabaseClient(
  supabaseUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co",
  supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ0bnd3aXdtbGpjd3pwc2F3ZG1mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzQ5MzE4OTMsImV4cCI6MjA1MDUwNzg5M30.cY4A4ZxqHA_1VRC-k6URVAHHkweHTR8FEYEzHYiu19A"
){
  install(Storage){
    resumable {
      cache = SettingsResumableCache()
    }
  }
}
const val BUCKET = "documents"
val resumableClient = supabaseClient.storage[BUCKET].resumable
val bucket = supabaseClient.storage[BUCKET]
package org.aryamahasangh.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import org.aryamahasangh.isAndroid

val localDev = true

val host = if(isAndroid()) "10.0.2.2" else "localhost"
var serverUrl = if(localDev) "http://${host}:4000" else "https://sandhya-anushthan-api.onrender.com"
var webSocketUrl = if(localDev) "ws://${host}:4000" else "wss://sandhya-anushthan-api.onrender.com"
val apolloClient = ApolloClient.Builder()
  .serverUrl("$serverUrl/graphql")
  .webSocketServerUrl("$webSocketUrl/subscriptions")
  .addHttpHeader("Access-Control-Allow-Origin", "*")
  //.addHttpInterceptor(SampleInterceptor())
  .build()


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
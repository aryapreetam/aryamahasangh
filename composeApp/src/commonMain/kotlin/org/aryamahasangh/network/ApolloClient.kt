package org.aryamahasangh.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import org.aryamahasangh.Platform
import org.aryamahasangh.getPlatform

val host = if(getPlatform() == Platform.ANDROID) "10.0.2.2" else "localhost"

val apolloClient = ApolloClient.Builder()
  .serverUrl("http://${host}:4000/graphql")
  .webSocketServerUrl("ws://${host}:4000/graphql")
//  .interceptors(listOf(MyApolloInterceptor()))
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
package com.aryamahasangh.nhost.graphql

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.aryamahasangh.nhost.auth.NHostAuth

/**
 * Apollo GraphQL interceptor that automatically adds NHost authentication headers
 */
class NHostApolloInterceptor(
  private val auth: NHostAuth
) : HttpInterceptor {

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    val accessToken = auth.getAccessToken()

    val newRequest = if (accessToken != null) {
      request.newBuilder()
        .addHeader("Authorization", "Bearer $accessToken")
        .build()
    } else {
      request
    }

    return chain.proceed(newRequest)
  }
}


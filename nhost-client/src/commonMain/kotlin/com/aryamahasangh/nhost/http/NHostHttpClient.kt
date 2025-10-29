package com.aryamahasangh.nhost.http

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HTTP clients for NHost
 */
internal object NHostHttpClientFactory {

  fun create(
    baseUrl: String,
    accessTokenProvider: suspend () -> String?
  ): HttpClient {
    return HttpClient {
      // Install JSON content negotiation
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
          encodeDefaults = true
          prettyPrint = false
          isLenient = true
        })
      }

      // Set default request configuration
      defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
      }

      // Install auth interceptor
      install(createAuthPlugin(accessTokenProvider))

      // Configure timeout
      install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
      }
    }
  }

  /**
   * Create an authentication plugin that adds Bearer token to requests
   */
  private fun createAuthPlugin(accessTokenProvider: suspend () -> String?) = createClientPlugin("NHostAuth") {
    onRequest { request, _ ->
      val token = accessTokenProvider()
      if (token != null) {
        request.header(HttpHeaders.Authorization, "Bearer $token")
      }
    }
  }
}


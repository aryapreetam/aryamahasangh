package org.aryamahasangh.network

import com.apollographql.apollo.ApolloClient
import org.aryamahasangh.Platform
import org.aryamahasangh.getPlatform

val host = if(getPlatform() == Platform.ANDROID) "10.0.2.2" else "localhost"

val apolloClient = ApolloClient.Builder()
  .serverUrl("http://${host}:4000/graphql")
  .addHttpHeader("Access-Control-Allow-Origin", "*")
  .build()
package com.aryamahasangh.network

import com.aryamahasangh.nhost.createNHostClient
import secrets.Secrets

val nhostClient = createNHostClient(Secrets.nhost_dev_graphql_url.dropLast(8))
val nhostApolloClient = nhostClient.createApolloClient(Secrets.nhost_dev_graphql_url)

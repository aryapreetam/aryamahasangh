package com.aryamahasangh.network

import com.aryamahasangh.nhost.createNHostClient

val nhostClient = createNHostClient("")
val nhostApolloClient = nhostClient.createApolloClient("")

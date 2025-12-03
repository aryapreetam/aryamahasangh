package com.aryamahasangh.network

import com.aryamahasangh.config.AppConfig
import com.aryamahasangh.isIos
import io.sentry.kotlin.multiplatform.Sentry
fun initializeSentry() {
  Sentry.init { options ->
    options.dsn = AppConfig.sentryDsn
    if(isIos) {
      options.attachStackTrace = false
      options.enableAppHangTracking = false
      options.enableAutoSessionTracking = false
    }
    // Adds request headers and IP for users, for more info visit:
    // https://docs.sentry.io/platforms/kotlin/guides/kotlin-multiplatform/data-management/data-collected/
    options.sendDefaultPii = true
  }
}

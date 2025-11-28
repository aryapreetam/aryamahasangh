package com.aryamahasangh.network

import com.aryamahasangh.config.AppConfig
import io.sentry.kotlin.multiplatform.Sentry
fun initializeSentry() {
  Sentry.init { options ->
    options.dsn = AppConfig.sentryDsn
    options.attachStackTrace = true
    options.enableAppHangTracking = false
    options.maxBreadcrumbs = 100
    // Adds request headers and IP for users, for more info visit:
    // https://docs.sentry.io/platforms/kotlin/guides/kotlin-multiplatform/data-management/data-collected/
    options.sendDefaultPii = true
  }
}

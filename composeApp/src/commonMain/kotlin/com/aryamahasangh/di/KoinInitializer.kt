package com.aryamahasangh.di

import com.aryamahasangh.auth.SessionManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform.getKoin

/**
 * Initializes Koin for dependency injection
 */
object KoinInitializer {
  fun start(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
      appDeclaration()
      modules(getAppModules())
    }
  }
}



object AppBootstrap {
  fun initialize() {
    MainScope().launch {
      val sessionManager: SessionManager = getKoin().get()
      sessionManager.initialize()
    }
  }
}

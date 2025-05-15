package org.aryamahasangh.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin for dependency injection
 */
object KoinInitializer {
  fun init(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
      appDeclaration()
      modules(getAppModules())
    }
  }
}

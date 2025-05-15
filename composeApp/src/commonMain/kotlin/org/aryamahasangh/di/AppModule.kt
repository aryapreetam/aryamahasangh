package org.aryamahasangh.di

import org.aryamahasangh.network.apolloClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Main application module for Koin dependency injection
 */
val appModule = module {
  // Provide Apollo Client
  single { apolloClient }
}

/**
 * Function to get all modules used in the application
 */
fun getAppModules(): List<Module> {
  return listOf(
    appModule,
    viewModelModule,
    repositoryModule
  )
}

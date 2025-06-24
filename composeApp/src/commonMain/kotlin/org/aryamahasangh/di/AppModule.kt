package org.aryamahasangh.di

import io.github.jan.supabase.graphql.graphql
import org.aryamahasangh.network.supabaseClient
import org.aryamahasangh.utils.FileUploadUtils
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Main application module for Koin dependency injection
 */
val appModule =
  module {
    // Provide Apollo Client
    single { supabaseClient.graphql.apolloClient }

    // Provide FileUploadUtils
    single { FileUploadUtils }
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

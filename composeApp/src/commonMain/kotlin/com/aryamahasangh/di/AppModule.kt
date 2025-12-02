package com.aryamahasangh.di

import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.features.gurukul.data.GurukulRepositoryImpl
import com.aryamahasangh.features.gurukul.data.ImageUploadRepository
import com.aryamahasangh.features.gurukul.data.ImageUploadRepositoryImpl
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationViewModel
import io.github.jan.supabase.graphql.graphql
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Main application module for Koin dependency injection
 */
val appModule =
  module {
    // CRITICAL iOS FIX: Use factory instead of single to prevent eager initialization
    // Sentry analysis confirmed that 'single' causes eager evaluation during Koin start on iOS,
    // triggering premature Keychain/NSUserDefaults access.
    // 'factory' ensures initialization happens ONLY when requested by AppDrawer (after UI is up).
    
    // Provide ApolloClient (factory)
    factory { com.aryamahasangh.network.supabaseClient.graphql.apolloClient }

    // Provide SupabaseClient (factory)
    factory { com.aryamahasangh.network.supabaseClient }
    
    // Provide SessionManager (factory)
    factory { SessionManager(get()) }

    // Provide NHost Apollo Client as a qualified dependency
    //single(named("nhost")) { nhostApolloClient }

    // NOTE: FileUploadUtils is a singleton object, access directly, don't register in Koin
    // Registering it would cause eager evaluation and potential Keychain access
  }

/**
 * Function to get all modules used in the application
 */
fun getAppModules(): List<Module> {
  return listOf(
    appModule,
    viewModelModule,
    repositoryModule,
    useCaseModule,
    GurukulCourseRegistrationModule
  )
}

val GurukulCourseRegistrationModule = module {
  // Use the existing ApolloClient instead of creating a new one
  singleOf(::ImageUploadRepositoryImpl) { bind<ImageUploadRepository>() }
  singleOf(::GurukulRepositoryImpl) { bind<GurukulRepository>() }
  factory { RegisterForCourseUseCase(get(), get()) }
  // We use GlobalMessageManager directly as it's a Kotlin object singleton
  factory { (activityId: String) ->
    CourseRegistrationViewModel(
      activityId = activityId,
      registerForCourseUseCase = get()
    )
  }
}

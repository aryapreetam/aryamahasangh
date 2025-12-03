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
import org.koin.core.module.dsl.factoryOf
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
    
    // Provide SupabaseClient (single)
    // CRITICAL: Use single to ensure only one instance is created.
    // Since it's inside a module and nothing requests it at startup, it's lazy.
    single { com.aryamahasangh.network.createAppSupabaseClient() }

    // Provide ApolloClient (factory)
    factory { get<io.github.jan.supabase.SupabaseClient>().graphql.apolloClient }

    // Provide FileUploadUtils (factory)
    factory { com.aryamahasangh.utils.FileUploadUtils(get()) }

    // Provide SessionManager (factory)
    factory { SessionManager(get()) }

    // Provide NHost Apollo Client as a qualified dependency
    //single(named("nhost")) { nhostApolloClient }
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
  factoryOf(::ImageUploadRepositoryImpl) { bind<ImageUploadRepository>() }
  factoryOf(::GurukulRepositoryImpl) { bind<GurukulRepository>() }
  factory { RegisterForCourseUseCase(get(), get()) }
  // We use GlobalMessageManager directly as it's a Kotlin object singleton
  factory { (activityId: String) ->
    CourseRegistrationViewModel(
      activityId = activityId,
      registerForCourseUseCase = get()
    )
  }
}

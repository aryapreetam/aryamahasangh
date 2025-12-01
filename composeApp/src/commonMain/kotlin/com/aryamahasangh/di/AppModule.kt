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
    // CRITICAL: Use factory with fully qualified access (no import!)
    // Importing supabaseClient at file level triggers initialization during file loading
    // Direct access causes Keychain access during Koin module definition (too early on iOS)
    single { com.aryamahasangh.network.supabaseClient.graphql.apolloClient }

    // Provide SupabaseClient
    single { com.aryamahasangh.network.supabaseClient }
    single { SessionManager(get()) }

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

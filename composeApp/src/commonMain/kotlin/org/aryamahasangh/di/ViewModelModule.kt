package org.aryamahasangh.di

import org.aryamahasangh.viewmodel.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Module for providing ViewModels
 */
val viewModelModule = module {
  // Provide ActivitiesViewModel
  factoryOf(::ActivitiesViewModel)

  // Provide LearningViewModel
  factoryOf(::LearningViewModel)

  // Provide AboutUsViewModel
  factoryOf(::AboutUsViewModel)

  // Provide OrganisationsViewModel
  factoryOf(::OrganisationsViewModel)

  // Provide AdmissionsViewModel
  factoryOf(::AdmissionsViewModel)

  // Provide JoinUsViewModel
  factoryOf(::JoinUsViewModel)
}

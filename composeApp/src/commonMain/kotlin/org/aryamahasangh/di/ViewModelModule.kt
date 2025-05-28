package org.aryamahasangh.di

import org.aryamahasangh.features.activities.ActivitiesViewModel
import org.aryamahasangh.features.arya_nirman.AryaNirmanViewModel
import org.aryamahasangh.features.arya_nirman.SatraRegistrationViewModel
import org.aryamahasangh.features.organisations.OrganisationsViewModel
import org.aryamahasangh.viewmodel.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Module for providing ViewModels
 */
val viewModelModule =
  module {
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
    factoryOf(::BookOrderViewModel)
    factoryOf(::AryaNirmanViewModel)
    factoryOf(::SatraRegistrationViewModel)
  }

package com.aryamahasangh.di

import com.aryamahasangh.features.activities.ActivitiesViewModel
import com.aryamahasangh.features.admin.AdminViewModel
import com.aryamahasangh.features.admin.FamilyViewModel
import com.aryamahasangh.features.admin.data.AryaSamajViewModel
import com.aryamahasangh.features.arya_nirman.AryaNirmanViewModel
import com.aryamahasangh.features.arya_nirman.SatraRegistrationViewModel
import com.aryamahasangh.features.organisations.OrganisationsViewModel
import com.aryamahasangh.viewmodel.*
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
    factoryOf(::AdminViewModel)
    factoryOf(::AryaSamajViewModel)
    factoryOf(::FamilyViewModel)
  }

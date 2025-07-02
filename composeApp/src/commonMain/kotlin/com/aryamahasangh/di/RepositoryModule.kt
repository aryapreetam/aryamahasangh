package com.aryamahasangh.di

import com.aryamahasangh.features.activities.ActivityRepository
import com.aryamahasangh.features.activities.ActivityRepositoryImpl
import com.aryamahasangh.features.admin.AdminRepository
import com.aryamahasangh.features.admin.AdminRepositoryImpl
import com.aryamahasangh.features.admin.FamilyRepository
import com.aryamahasangh.features.admin.FamilyRepositoryImpl
import com.aryamahasangh.features.admin.data.AryaSamajRepository
import com.aryamahasangh.features.admin.data.AryaSamajRepositoryImpl
import com.aryamahasangh.features.arya_nirman.AryaNirmanRepository
import com.aryamahasangh.features.arya_nirman.AryaNirmanRepositoryImpl
import com.aryamahasangh.features.organisations.OrganisationsRepository
import com.aryamahasangh.features.organisations.OrganisationsRepositoryImpl
import com.aryamahasangh.repository.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Module for providing repositories
 */
val repositoryModule =
  module {
    // Provide ActivityRepository
    singleOf(::ActivityRepositoryImpl) { bind<ActivityRepository>() }

    // Provide LearningRepository
    singleOf(::LearningRepositoryImpl) { bind<LearningRepository>() }

    // Provide AboutUsRepository
    singleOf(::AboutUsRepositoryImpl) { bind<AboutUsRepository>() }

    // Provide OrganisationsRepository
    singleOf(::OrganisationsRepositoryImpl) { bind<OrganisationsRepository>() }

    // Provide AdmissionsRepository
    singleOf(::AdmissionsRepositoryImpl) { bind<AdmissionsRepository>() }

    // Provide JoinUsRepository
    singleOf(::JoinUsRepositoryImpl) { bind<JoinUsRepository>() }

    singleOf(::BookOrderRepositoryImpl) { bind<BookOrderRepository>() }
    singleOf(::AryaNirmanRepositoryImpl) { bind<AryaNirmanRepository>() }
    singleOf(::AdminRepositoryImpl) { bind<AdminRepository>() }
    singleOf(::AryaSamajRepositoryImpl) { bind<AryaSamajRepository>() }
    singleOf(::FamilyRepositoryImpl) { bind<FamilyRepository>() }
  }

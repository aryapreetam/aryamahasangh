package org.aryamahasangh.di

import org.aryamahasangh.features.activities.ActivityRepository
import org.aryamahasangh.features.activities.ActivityRepositoryImpl
import org.aryamahasangh.features.arya_nirman.AryaNirmanRepository
import org.aryamahasangh.features.arya_nirman.AryaNirmanRepositoryImpl
import org.aryamahasangh.features.organisations.OrganisationsRepository
import org.aryamahasangh.features.organisations.OrganisationsRepositoryImpl
import org.aryamahasangh.repository.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Module for providing repositories
 */
val repositoryModule = module {
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
}

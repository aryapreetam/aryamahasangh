package com.aryamahasangh.di

import com.aryamahasangh.features.about_us.data.AboutUsRepositoryImpl
import com.aryamahasangh.features.about_us.domain.repository.AboutUsRepository
import com.aryamahasangh.features.activities.ActivityRepository
import com.aryamahasangh.features.activities.ActivityRepositoryImpl
import com.aryamahasangh.features.admin.AdminRepository
import com.aryamahasangh.features.admin.AdminRepositoryImpl
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajRepository
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajRepositoryImpl
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajSelectorRepository
import com.aryamahasangh.features.admin.aryasamaj.data.AryaSamajSelectorRepositoryImpl
import com.aryamahasangh.features.admin.family.FamilyRepository
import com.aryamahasangh.features.admin.family.FamilyRepositoryImpl
import com.aryamahasangh.features.admin.member.MembersSelectorRepository
import com.aryamahasangh.features.admin.member.data.MembersSelectorRepositoryImpl
import com.aryamahasangh.features.arya_nirman.AryaNirmanRepository
import com.aryamahasangh.features.arya_nirman.AryaNirmanRepositoryImpl
import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.features.gurukul.data.GurukulRepositoryImpl
import com.aryamahasangh.features.organisations.OrganisationsRepository
import com.aryamahasangh.features.organisations.OrganisationsRepositoryImpl
import com.aryamahasangh.features.public_arya_samaj.AryaSamajHomeRepository
import com.aryamahasangh.features.public_arya_samaj.AryaSamajHomeRepositoryImpl
import com.aryamahasangh.repository.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Module for providing repositories
 */
val repositoryModule =
  module {
    // CRITICAL iOS FIX: Use factoryOf instead of singleOf
    // Repositories depend on SupabaseClient/ApolloClient. If defined as 'single', they can trigger
    // eager initialization of the client during Koin start on iOS, causing the crash.
    // 'factory' ensures they are created only when ViewModels ask for them (after UI load).
    
    // Provide ActivityRepository
    factoryOf(::ActivityRepositoryImpl) { bind<ActivityRepository>() }

    // Provide LearningRepository
    factoryOf(::LearningRepositoryImpl) { bind<LearningRepository>() }

    // Provide AboutUsRepository
    factoryOf(::AboutUsRepositoryImpl) { bind<AboutUsRepository>() }
    // Provide OrganisationsRepository
    factoryOf(::OrganisationsRepositoryImpl) { bind<OrganisationsRepository>() }

    // Provide AdmissionsRepository
    factoryOf(::AdmissionsRepositoryImpl) { bind<AdmissionsRepository>() }

    // Provide JoinUsRepository
    factoryOf(::JoinUsRepositoryImpl) { bind<JoinUsRepository>() }

    factoryOf(::BookOrderRepositoryImpl) { bind<BookOrderRepository>() }
    factoryOf(::AryaNirmanRepositoryImpl) { bind<AryaNirmanRepository>() }
    factoryOf(::AdminRepositoryImpl) { bind<AdminRepository>() }
    factoryOf(::AryaSamajRepositoryImpl) { bind<AryaSamajRepository>() }
    factoryOf(::AryaSamajSelectorRepositoryImpl) { bind<AryaSamajSelectorRepository>() }
    factoryOf(::AryaSamajHomeRepositoryImpl) { bind<AryaSamajHomeRepository>() }
    factoryOf(::FamilyRepositoryImpl) { bind<FamilyRepository>() }
    factoryOf(::MembersSelectorRepositoryImpl) { bind<MembersSelectorRepository>() }
    factoryOf(::GurukulRepositoryImpl) { bind<GurukulRepository>() }
  }

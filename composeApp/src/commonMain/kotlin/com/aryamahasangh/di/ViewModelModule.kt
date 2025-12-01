package com.aryamahasangh.di

import com.aryamahasangh.features.about_us.ui.AboutUsViewModel
import com.aryamahasangh.viewmodel.LoginViewModel
import com.aryamahasangh.features.activities.ActivitiesViewModel
import com.aryamahasangh.features.admin.AdminViewModel
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajSelectorViewModel
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajViewModel
import com.aryamahasangh.features.admin.family.FamilyViewModel
import com.aryamahasangh.features.admin.member.MembersSelectorViewModel
import com.aryamahasangh.features.arya_nirman.AryaNirmanViewModel
import com.aryamahasangh.features.arya_nirman.SatraRegistrationViewModel
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationsReceivedViewModel
import com.aryamahasangh.features.gurukul.viewmodel.UpcomingCoursesViewModel
import com.aryamahasangh.features.organisations.OrganisationsViewModel
import com.aryamahasangh.features.public_arya_samaj.AryaSamajHomeViewModel
import com.aryamahasangh.type.GenderFilter
import com.aryamahasangh.viewmodel.AdmissionsViewModel
import com.aryamahasangh.viewmodel.BookOrderViewModel
import com.aryamahasangh.viewmodel.JoinUsViewModel
import com.aryamahasangh.viewmodel.LearningViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Module for providing ViewModels
 */
val viewModelModule =
  module {
    // Provide ActivitiesViewModel
    factoryOf(::ActivitiesViewModel)

    // Provide LoginViewModel
    factoryOf(::LoginViewModel)

    // Provide LearningViewModel
    factoryOf(::LearningViewModel)

    // Provide AboutUsViewModel (new feature)
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
    factoryOf(::AryaSamajSelectorViewModel)
    factoryOf(::AryaSamajHomeViewModel)
    factoryOf(::FamilyViewModel)
    factoryOf(::MembersSelectorViewModel)
    factory { (gender: GenderFilter) -> UpcomingCoursesViewModel(get(), gender) }
    factory { (gender: GenderFilter) -> CourseRegistrationsReceivedViewModel(get(), gender) }
  }

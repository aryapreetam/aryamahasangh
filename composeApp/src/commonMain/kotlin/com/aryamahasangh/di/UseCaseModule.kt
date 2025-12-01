package com.aryamahasangh.di

import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationByNameUseCase
import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationNamesUseCase
import com.aryamahasangh.features.gurukul.domain.usecase.GetCoursesUseCase
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
  factoryOf(::GetCoursesUseCase)
  // Note: RegisterForCourseUseCase takes 2 params, keeping as factory{}
  factory { RegisterForCourseUseCase(get(), get()) }

  // About Us feature use cases
  factoryOf(::GetOrganisationByNameUseCase)
  factoryOf(::GetOrganisationNamesUseCase)
}

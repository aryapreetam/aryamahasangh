package com.aryamahasangh.di

import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationByNameUseCase
import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationNamesUseCase
import com.aryamahasangh.features.gurukul.domain.usecase.GetCoursesUseCase
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import org.koin.dsl.module

val useCaseModule = module {
  factory { GetCoursesUseCase(get()) }
  factory { RegisterForCourseUseCase(get(), get()) }

  // About Us feature use cases
  factory { GetOrganisationByNameUseCase(get()) }
  factory { GetOrganisationNamesUseCase(get()) }
}

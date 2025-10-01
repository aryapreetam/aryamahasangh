package com.aryamahasangh.di

import com.aryamahasangh.features.gurukul.domain.usecase.GetCoursesUseCase
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import org.koin.dsl.module

val useCaseModule = module {
  factory { GetCoursesUseCase(get()) }
  factory { RegisterForCourseUseCase(get(), get()) }
}

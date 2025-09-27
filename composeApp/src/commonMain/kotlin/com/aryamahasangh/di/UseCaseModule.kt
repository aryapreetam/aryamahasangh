package com.aryamahasangh.di

import com.aryamahasangh.features.gurukul.domain.usecase.GetCoursesUseCase
import org.koin.dsl.module

val useCaseModule = module {
  factory { GetCoursesUseCase(get()) }
}

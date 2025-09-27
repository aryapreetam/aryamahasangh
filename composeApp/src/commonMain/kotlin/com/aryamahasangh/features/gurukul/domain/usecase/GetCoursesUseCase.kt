package com.aryamahasangh.features.gurukul.domain.usecase

import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.features.gurukul.domain.models.Course
import com.aryamahasangh.type.GenderFilter

// Stateless domain usecase for fetching all courses filtered by gender.
// Encapsulates repository and provides base for orchestration or business rules if needed in future.
class GetCoursesUseCase(private val repository: GurukulRepository) {
  /**
   * Loads all available courses of type COURSE filtered by gender.
   * Pure suspend function for use in ViewModel/Molecule presenter.
   */
  suspend operator fun invoke(gender: GenderFilter): List<Course> {
    return repository.getCourses(gender)
  }
}

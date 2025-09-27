package com.aryamahasangh.features.gurukul.data

import com.aryamahasangh.features.gurukul.domain.models.Course
import com.aryamahasangh.type.GenderFilter

// Repository contract for Gurukul feature data operations.
// Future phases will add more methods as needed.
interface GurukulRepository {
  /**
   * Load all available courses (type=COURSE) filtered by gender.
   * This uses Apollo generated GenderFilter type.
   * Should return an immutable list of Course domain models.
   */
  suspend fun getCourses(gender: GenderFilter): List<Course>
}

package com.aryamahasangh.features.gurukul.data

import com.aryamahasangh.CourseRegistrationsForActivityQuery
import com.aryamahasangh.RegisterForCourseMutation
import com.aryamahasangh.features.gurukul.domain.models.Course
import com.aryamahasangh.type.GenderFilter

// Repository contract for Gurukul feature data operations.
// Future phases will add more methods as needed.
interface GurukulRepository {
  /**
   * Load upcoming courses only (type=COURSE, startDatetime > now) filtered by gender.
   * This uses Apollo generated GenderFilter type.
   * Should return an immutable list of Course domain models.
   */
  suspend fun getCourses(gender: GenderFilter): List<Course>

  /**
   * Load ALL courses (past, present, and future) filtered by gender.
   * Results are sorted by start date in descending order (most recent first).
   * Used for admin screens where viewing all courses is needed.
   */
  suspend fun getAllCourses(gender: GenderFilter): List<Course>

  suspend fun registerForCourse(mutation: RegisterForCourseMutation): Result<Unit>

  suspend fun getCourseRegistrationsForActivity(activityId: String): Result<List<CourseRegistrationsForActivityQuery.Node>>
}

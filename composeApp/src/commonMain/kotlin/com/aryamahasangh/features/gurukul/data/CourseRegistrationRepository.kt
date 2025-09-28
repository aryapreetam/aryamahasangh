// ... existing code ...
package com.aryamahasangh.features.gurukul.data

import com.aryamahasangh.RegisterForCourseMutation

interface CourseRegistrationRepository {
  suspend fun registerForCourse(mutation: RegisterForCourseMutation): Result<Unit>
  // TODO: Define error type if richer reporting/interface is needed
}
// ... existing code ...

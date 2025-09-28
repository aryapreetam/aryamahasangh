package com.aryamahasangh.features.gurukul.data

import com.apollographql.apollo.ApolloClient
import com.aryamahasangh.RegisterForCourseMutation

class CourseRegistrationRepositoryImpl(
  private val apolloClient: ApolloClient
) : CourseRegistrationRepository {
  override suspend fun registerForCourse(mutation: RegisterForCourseMutation): Result<Unit> {
    return try {
      val response = apolloClient.mutation(mutation).execute()
      if (response.hasErrors()) {
        Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "पंजीकरण विफल"))
      } else {
        Result.success(Unit)
      }
    } catch (e: Exception) {
      Result.failure(Exception("पंजीकरण विफल: ${e.message}"))
    }
  }
}

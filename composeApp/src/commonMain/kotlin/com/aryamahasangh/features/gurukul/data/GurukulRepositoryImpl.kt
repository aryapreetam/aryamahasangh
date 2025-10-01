package com.aryamahasangh.features.gurukul.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.apolloStore
import com.aryamahasangh.CourseRegistrationsForActivityQuery
import com.aryamahasangh.LoadAllCoursesQuery
import com.aryamahasangh.RegisterForCourseMutation
import com.aryamahasangh.features.gurukul.domain.models.Address
import com.aryamahasangh.features.gurukul.domain.models.Course
import com.aryamahasangh.type.GenderFilter

class GurukulRepositoryImpl(
  private val apolloClient: ApolloClient
) : GurukulRepository {
  override suspend fun getCourses(gender: GenderFilter): List<Course> {
    val query = LoadAllCoursesQuery.Builder()
      .gender(gender)
      .build()
    val response = apolloClient.query(query).execute()
    // Map Apollo response data to list of Course domain models
    return response.data?.activitiesCollection?.edges?.mapNotNull { edge ->
      val node = edge.node
      Course(
        id = node.id,
        name = node.name,
        shortDescription = node.shortDescription,
        startDatetime = node.startDatetime,
        endDatetime = node.endDatetime,
        allowedGender = node.allowedGender?.name ?: "",
        address = node.address?.let {
          Address(
            district = it.district,
            state = it.state,
            latitude = it.latitude,
            longitude = it.longitude
          )
        },
        capacity = node.capacity
      )
    }.orEmpty()
  }

  override suspend fun registerForCourse(mutation: RegisterForCourseMutation): Result<Unit> {
    return try {
      val response = apolloClient.mutation(mutation).execute()
      if (response.hasErrors()) {
        Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "पंजीकरण विफल"))
      } else {
        apolloClient.apolloStore.clearAll()
        Result.success(Unit)
      }
    } catch (e: Exception) {
      Result.failure(Exception("पंजीकरण विफल: ${e.message}"))
    }
  }

  override suspend fun getCourseRegistrationsForActivity(activityId: String): Result<List<CourseRegistrationsForActivityQuery.Node>> {
    return try {
      val response = apolloClient.query(CourseRegistrationsForActivityQuery(activityId)).execute()
      if (response.hasErrors()) {
        Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "त्रुटि आ गई"))
      } else {
        val userList = response.data?.courseRegistrationsCollection?.edges?.map { edge ->
          edge.node
        } ?: emptyList()
        Result.success(userList)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

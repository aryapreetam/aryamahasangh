package com.aryamahasangh.features.gurukul.data

import com.apollographql.apollo.ApolloClient
import com.aryamahasangh.LoadAllCoursesQuery
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
}

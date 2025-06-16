package org.aryamahasangh.features.arya_nirman

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.aryamahasangh.RegisterForSatrMutation
import org.aryamahasangh.UpcomingSatrActivitiesQuery
import org.aryamahasangh.features.activities.toDomain
import org.aryamahasangh.network.supabaseClient
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

interface AryaNirmanRepository {
  suspend fun getUpcomingActivities(): Flow<Result<List<UpcomingActivity>>>

  fun registerForActivity(
    activityId: String,
    data: RegistrationData
  ): Flow<Result<Boolean>>

  fun getRegistrationCounts(): Flow<Map<String, Int>>
  suspend fun getRegistrationCountByActivityId(activityId: String): Long?
}

class AryaNirmanRepositoryImpl(private val apolloClient: ApolloClient) : AryaNirmanRepository {
  override suspend fun getUpcomingActivities(): Flow<Result<List<UpcomingActivity>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(UpcomingSatrActivitiesQuery(currentDateTime = Clock.System.now())).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.activitiesCollection?.edges?.map { upcomingActivityData ->
            upcomingActivityData.node.let {
              UpcomingActivity(
                id = it.id,
                name = it.name!!,
                genderAllowed = it.allowed_gender.toDomain(),
                isFull = (it.satr_registrationCollection?.edges?.size ?: 0) >= it.capacity!!,
                startDateTime = it.start_datetime!!.toLocalDateTime(TimeZone.currentSystemDefault()),
                endDateTime = it.end_datetime!!.toLocalDateTime(TimeZone.currentSystemDefault()),
                district = it.district!!,
                state = it.state!!,
                latitude = it.latitude!!,
                longitude = it.longitude!!,
                capacity = it.capacity
              )
            }
          } ?: emptyList()
        }
      emit(result)
    }

  override fun registerForActivity(
    activityId: String,
    data: RegistrationData
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              RegisterForSatrMutation(
                fullName = data.fullName,
                gender = data.gender.name,
                mobile = data.phoneNumber,
                aadharNo = data.aadharNumber,
                educationalQualification = data.education,
                address = data.fullAddress,
                inspirationSource = data.inspirationSource,
                inspirationSourceName = Optional.presentIfNotNull(data.inspirationDetailName),
                inspirationSourceNo = Optional.presentIfNotNull(data.inspirationDetailPhone),
                hasTrainedAryaInFamily = data.hasTrainedAryaInFamily,
                trainedAryaName = Optional.presentIfNotNull(data.trainedAryaName),
                trainedAryaNo = Optional.presentIfNotNull(data.trainedAryaPhone),
                activityId = activityId
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.insertIntosatr_registrationCollection?.affectedCount!! > 0
        }
      emit(result)
    }

  @OptIn(SupabaseExperimental::class)
  override fun getRegistrationCounts(): Flow<Map<String, Int>> {
    return supabaseClient
      .from("satr_registration")
      .selectAsFlow(
        primaryKeys = listOf(SatrRegistrationCount::id)
      ).map { registrations ->
        registrations.groupingBy { it.activity_id }.eachCount()
      }
  }

  override suspend fun getRegistrationCountByActivityId(activityId: String): Long? {
    return supabaseClient
      .from("satr_registration")
      .select {
        count(Count.EXACT)
        filter {
          filter(
            column ="activity_id",
            operator = FilterOperator.EQ,
            value = activityId
            )
        }
      }.countOrNull()
  }
}

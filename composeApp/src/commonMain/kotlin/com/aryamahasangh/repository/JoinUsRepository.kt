package com.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import com.aryamahasangh.AppLabelQuery
import com.aryamahasangh.OrganisationalActivitiesQuery
import com.aryamahasangh.UpdateJoinUsLabelMutation
import com.aryamahasangh.fragment.ActivityWithStatus
import com.aryamahasangh.fragment.OrganisationalActivityShort
import com.aryamahasangh.type.ActivitiesFilter
import com.aryamahasangh.type.ActivitiesWithStatus
import com.aryamahasangh.type.ActivitiesWithStatusFilter
import com.aryamahasangh.type.ActivityTypeFilter
import com.aryamahasangh.type.DatetimeFilter
import com.aryamahasangh.type.StringFilter
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import com.aryamahasangh.type.ActivityType as ApolloActivityType

/**
 * Repository for handling join us related operations
 */
interface JoinUsRepository {
  /**
   * Get filtered activities
   */
  fun getFilteredActivities(
    state: String,
    district: String = ""
  ): Flow<Result<List<ActivityWithStatus>>>

  fun getJoinUsLabel(): Flow<Result<String>>

  fun updateLabel(label: String): Flow<Result<Boolean>>
}

/**
 * Implementation of JoinUsRepository that uses Apollo GraphQL client
 */
class JoinUsRepositoryImpl(private val apolloClient: ApolloClient) : JoinUsRepository {
  override fun getFilteredActivities(
    state: String,
    district: String
  ): Flow<Result<List<ActivityWithStatus>>> =
    flow {
      emit(Result.Loading)
      val startTimeFilter =
        Optional.present(
          value =
            DatetimeFilter(
              gt =
                Optional.present(
                  value = Clock.System.now()
                )
            )
        )
      val result =
        safeCall {
          val response =
            apolloClient.query(
              OrganisationalActivitiesQuery(
                filter =
                  Optional.present(
                    value =
                      ActivitiesWithStatusFilter(
                        type = Optional.present(ActivityTypeFilter(eq = Optional.present(ApolloActivityType.SESSION))),
                        state = Optional.present(StringFilter(eq = Optional.present(state))),
                        district =
                          if (district.isNotEmpty()) {
                            Optional.present(
                              StringFilter(eq = Optional.present(district))
                            )
                          } else {
                            Optional.absent()
                          },
                        startDatetime = startTimeFilter
                      )
                  )
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.activitiesWithStatusCollection?.edges?.map { it.node.activityWithStatus } ?: emptyList()
        }
      emit(result)
    }

  override fun getJoinUsLabel(): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val res =
        safeCall {
          val resp = apolloClient.query(AppLabelQuery(labelKey = "join_us")).execute()
          if (resp.hasErrors()) {
            throw Exception(resp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          resp.data?.appLabelsCollection?.edges?.firstOrNull()?.node?.labelValue ?: ""
        }
      emit(res)
    }

  override fun updateLabel(label: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val res =
        safeCall {
          val resp =
            apolloClient.mutation(
              UpdateJoinUsLabelMutation(input = label)
            ).execute()
          if (resp.hasErrors()) {
            throw Exception(resp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          resp.data?.updateAppLabelsCollection?.affectedCount!! > 0
        }
      emit(res)
    }
  }
}

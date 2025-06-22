package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.aryamahasangh.AppLabelQuery
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.UpdateJoinUsLabelMutation
import org.aryamahasangh.fragment.OrganisationalActivityShort
import org.aryamahasangh.type.ActivitiesFilter
import org.aryamahasangh.type.ActivityTypeFilter
import org.aryamahasangh.type.DatetimeFilter
import org.aryamahasangh.type.StringFilter
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall
import org.aryamahasangh.type.ActivityType as ApolloActivityType

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
  ): Flow<Result<List<OrganisationalActivityShort>>>

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
  ): Flow<Result<List<OrganisationalActivityShort>>> =
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
                      ActivitiesFilter(
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
          response.data?.activitiesCollection?.edges?.map { it.node.organisationalActivityShort } ?: emptyList()
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

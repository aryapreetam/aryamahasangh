package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.aryamahasangh.AppLabelQuery
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.UpdateJoinUsLabelMutation
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.features.activities.OrganisationalActivityShort
import org.aryamahasangh.features.activities.camelCased
import org.aryamahasangh.type.*
import org.aryamahasangh.util.Result

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
      println(startTimeFilter)
      val result =
        ErrorHandler.safeCall {
          val response =
            apolloClient.query(
              OrganisationalActivitiesQuery(
                filter =
                  Optional.present(
                    value =
                      ActivitiesFilter(
                        type = Optional.present(Activity_typeFilter(eq = Optional.present(Activity_type.SESSION))),
                        state = Optional.present(StringFilter(eq = Optional.present(state))),
                        district =
                          if (district.isNotEmpty()) {
                            Optional.present(
                              StringFilter(eq = Optional.present(district))
                            )
                          } else {
                            Optional.absent()
                          },
                        start_datetime = startTimeFilter
                      )
                  )
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.activitiesCollection?.edges?.map { it.node.organisationalActivityShort.camelCased() } ?: emptyList()
        }

      emit(result)
    }

  override fun getJoinUsLabel(): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val res =
        ErrorHandler.safeCall {
          val resp = apolloClient.query(AppLabelQuery(labelKey = "join_us")).execute()
          if (resp.hasErrors()) {
            throw Exception(resp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          resp.data?.app_labelsCollection?.edges[0]?.node?.label_value ?: ""
        }
      emit(res)
    }

  override fun updateLabel(label: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val res =
        ErrorHandler.safeCall {
          val resp =
            apolloClient.mutation(
              UpdateJoinUsLabelMutation(input = label)
            ).execute()
          if (resp.hasErrors()) {
            throw Exception(resp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          resp.data?.updateapp_labelsCollection?.affectedCount!! > 0
        }
      emit(res)
    }
  }
}

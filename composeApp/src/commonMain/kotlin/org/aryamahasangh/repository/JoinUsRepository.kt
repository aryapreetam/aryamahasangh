package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.LabelQuery
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.UpdateJoinUsLabelMutation
import org.aryamahasangh.type.ActivityFilterInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling join us related operations
 */
interface JoinUsRepository {
  /**
   * Get filtered activities
   */
  fun getFilteredActivities(filter: ActivityFilterInput): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>>
  fun getJoinUsLabel(): Flow<Result<String>>
  fun updateLabel(label: String): Flow<Result<Boolean>>
}

/**
 * Implementation of JoinUsRepository that uses Apollo GraphQL client
 */
class JoinUsRepositoryImpl(private val apolloClient: ApolloClient) : JoinUsRepository {

  override fun getFilteredActivities(filter: ActivityFilterInput): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(
        OrganisationalActivitiesQuery(filter = Optional.present(filter))
      ).execute()
      
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.organisationalActivities ?: emptyList()
    }

    emit(result)
  }

  override fun getJoinUsLabel(): Flow<Result<String>> = flow {
    emit(Result.Loading)
    val res = safeCall {
      val resp = apolloClient.query(LabelQuery(key = "join_us")).execute()
      if (resp.hasErrors()) {
        throw Exception(resp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      resp.data?.label ?: ""
    }
    emit(res)
  }

  override fun updateLabel(label: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val res = safeCall {
        val resp = apolloClient.mutation(
          UpdateJoinUsLabelMutation(label = label)
        ).execute()
        if (resp.hasErrors()) {
          throw Exception(resp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        resp.data?.updateJoinUsLabel ?: false
      }
      emit(res)
    }
  }
}
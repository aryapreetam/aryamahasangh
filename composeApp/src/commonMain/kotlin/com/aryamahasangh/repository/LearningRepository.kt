package com.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import com.aryamahasangh.LearningItemQuery
import com.aryamahasangh.LearningItemsQuery
import com.aryamahasangh.type.LearningFilter
import com.aryamahasangh.type.StringFilter
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall

/**
 * Repository for handling learning-related operations
 */
interface LearningRepository {
  /**
   * Get all learning items
   */
  fun getLearningItems(): Flow<Result<List<LearningItem>>>

  /**
   * Get learning item details by ID
   */
  fun getLearningItemDetail(id: String): Flow<Result<LearningItem>>
}

@Serializable
data class LearningItem(
  val id: String,
  val title: String,
  val description: String = "",
  val url: String = "",
  val thumbnailUrl: String = "",
  val videoId: String = ""
)

/**
 * Implementation of LearningRepository that uses Apollo GraphQL client
 */
class LearningRepositoryImpl(private val apolloClient: ApolloClient) : LearningRepository {
  override fun getLearningItems(): Flow<Result<List<LearningItem>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(LearningItemsQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.learningCollection?.edges?.map {
            val item = it.node
            LearningItem(
              id = item.id,
              title = item.title!!,
              thumbnailUrl = item.thumbnailUrl!!
            )
          } ?: emptyList()
        }

      emit(result)
    }

  override fun getLearningItemDetail(id: String): Flow<Result<LearningItem>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response =
            apolloClient.query(
              LearningItemQuery(
                filter =
                  Optional.present(
                    LearningFilter(
                      id =
                        Optional.present(
                          StringFilter(eq = Optional.present(id))
                        )
                    )
                  )
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.learningCollection?.edges?.map {
            val item = it.node
            LearningItem(
              id = item.id,
              title = item.title!!,
              thumbnailUrl = item.thumbnailUrl ?: "",
              description = item.description ?: "",
              url = item.url ?: "",
              videoId = item.videoId ?: ""
            )
          }[0] ?: throw Exception("Learning item not found")
        }

      emit(result)
    }
}

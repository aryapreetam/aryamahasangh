package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import org.aryamahasangh.LearningItemQuery
import org.aryamahasangh.LearningItemsQuery
import org.aryamahasangh.type.LearningFilter
import org.aryamahasangh.type.StringFilter
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

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
  suspend fun getLearningItemDetail(id: String): Result<LearningItem>
}

data class LearningItem(
  val id: String,
  val title: String,
  val description: String = "",
  val url: String = "",
  @SerialName("thumbnail_url")
  val thumbnailUrl: String = "",
  @SerialName("video_id")
  val videoId: String = ""
)

/**
 * Implementation of LearningRepository that uses Apollo GraphQL client
 */
class LearningRepositoryImpl(private val apolloClient: ApolloClient) : LearningRepository {

  override fun getLearningItems(): Flow<Result<List<LearningItem>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(LearningItemsQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.learningCollection?.edges?.map {
        val item = it.node
        LearningItem(
          id = item.id,
          title = item.title!!,
          thumbnailUrl = item.thumbnail_url!!,
        )
      } ?: emptyList()
    }

    emit(result)
  }

  override suspend fun getLearningItemDetail(id: String): Result<LearningItem> {
    return safeCall {
      val response = apolloClient.query(LearningItemQuery(
        filter = Optional.present(
          LearningFilter(id = Optional.present(
            StringFilter(eq = Optional.present(id))
          ))))
      ).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.learningCollection?.edges?.map {
        val item = it.node
        LearningItem(
          id = item.id,
          title = item.title!!,
          thumbnailUrl = item.thumbnail_url ?: "",
          description = item.description ?: "",
          url = item.url ?: "",
          videoId = item.video_id ?: "",
        )
      }[0] ?: throw Exception("Learning item not found")
    }
  }
}
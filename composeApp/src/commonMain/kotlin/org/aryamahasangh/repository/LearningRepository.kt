package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.LearningsItemsQuery
import org.aryamahasangh.LearningItemDetailQuery
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling learning-related operations
 */
interface LearningRepository {
  /**
   * Get all learning items
   */
  fun getLearningItems(): Flow<Result<List<LearningsItemsQuery.LearningItem>>>

  /**
   * Get learning item details by ID
   */
  suspend fun getLearningItemDetail(id: String): Result<LearningItemDetailQuery.LearningItem>
}

/**
 * Implementation of LearningRepository that uses Apollo GraphQL client
 */
class LearningRepositoryImpl(private val apolloClient: ApolloClient) : LearningRepository {

  override fun getLearningItems(): Flow<Result<List<LearningsItemsQuery.LearningItem>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(LearningsItemsQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.learningItems ?: emptyList()
    }

    emit(result)
  }

  override suspend fun getLearningItemDetail(id: String): Result<LearningItemDetailQuery.LearningItem> {
    return safeCall {
      val response = apolloClient.query(LearningItemDetailQuery(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.learningItem ?: throw Exception("Learning item not found")
    }
  }
}
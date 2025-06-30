package com.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.aryamahasangh.OrganisationQuery
import com.aryamahasangh.OrganisationNamesQuery
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.features.organisations.OrganisationalMember
import com.aryamahasangh.type.OrganisationFilter
import com.aryamahasangh.type.StringFilter
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall

/**
 * Data class representing organization name and id
 */
data class OrganisationName(
  val id: String,
  val name: String
)

/**
 * Repository for handling about us related operations
 */
interface AboutUsRepository {
  /**
   * Get organisation details by name
   */
  fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>>

  /**
   * Get all organisation names
   */
  fun getOrganisationNames(): Flow<Result<List<OrganisationName>>>
}

/**
 * Implementation of AboutUsRepository that uses Apollo GraphQL client
 */
class AboutUsRepositoryImpl(private val apolloClient: ApolloClient) : AboutUsRepository {
  override fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response =
            apolloClient.query(
              OrganisationQuery(
                filter =
                  Optional.present(
                    OrganisationFilter(name = Optional.present(StringFilter(eq = Optional.present(name))))
                  )
              )
            ).execute()

          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            throw Exception(errorMessage)
          }

          response.data?.organisationCollection?.edges?.map {
            OrganisationDetail(
              id = it.node.id,
              name = it.node.name!!,
              description = it.node.description!!,
              logo = it.node.logo,
              members =
                it.node.organisationalMemberCollection?.edges?.map {
                  val (id, post, priority, _member) = it.node
                  val member = _member!!
                  OrganisationalMember(
                    id = id,
                    post = post!!,
                    priority = priority!!,
                    member =
                      Member(
                        id = member.id,
                        name = member.name!!,
                        profileImage = member.profileImage ?: "",
                        phoneNumber = member.phoneNumber ?: ""
                      )
                  )
                } ?: emptyList()
            )
          }?.firstOrNull() ?: throw Exception("Organisation not found")
        }

      emit(result)
    }

  override fun getOrganisationNames(): Flow<Result<List<OrganisationName>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationNamesQuery()).execute()

          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            throw Exception(errorMessage)
          }

          response.data?.organisationCollection?.edges?.map { edge ->
            OrganisationName(
              id = edge.node.id,
              name = edge.node.name
            )
          } ?: emptyList()
        }

      emit(result)
    }
}

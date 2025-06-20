package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.features.organisations.OrganisationDetail
import org.aryamahasangh.features.organisations.OrganisationalMember
import org.aryamahasangh.type.OrganisationFilter
import org.aryamahasangh.type.StringFilter
import org.aryamahasangh.util.Result

/**
 * Repository for handling about us related operations
 */
interface AboutUsRepository {
  /**
   * Get organisation details by name
   */
  fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>>
}

/**
 * Implementation of AboutUsRepository that uses Apollo GraphQL client
 */
class AboutUsRepositoryImpl(private val apolloClient: ApolloClient) : AboutUsRepository {
  override fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>> =
    flow {
      emit(Result.Loading)

      val result = ErrorHandler.safeCall {
        try {
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
            println("DEBUG - GraphQL Error: $errorMessage")
            throw Exception(errorMessage)
          }

          response.data?.organisationCollection?.edges?.map {
            OrganisationDetail(
              id = it.node.id,
              name = it.node.name!!,
              description = it.node.description!!,
              logo = it.node.logo,
              members =
                it.node.organisational_memberCollection?.edges?.map {
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
                        profileImage = member.profile_image ?: "",
                        phoneNumber = member.phone_number ?: ""
                      )
                  )
                }!!
            )
          }[0] ?: throw Exception("Organisation not found")

        } catch (e: Exception) {
          println("DEBUG - Caught exception in repository: ${e::class.simpleName}, Message: ${e.message}")
          // Re-throw to let ErrorHandler.safeCall handle it
          throw e
        }
      }

      emit(result)
    }
}

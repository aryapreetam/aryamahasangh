package org.aryamahasangh.features.organisations

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.*
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.type.OrganisationFilter
import org.aryamahasangh.type.StringFilter
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling organisations-related operations
 */
interface OrganisationsRepository {
  /**
   * Get all organisations
   */
  fun getOrganisations(): Flow<Result<List<OrganisationWithDescription>>>

  /**
   * Get organisation details by name
   */
  fun getOrganisationById(id: String): Flow<Result<OrganisationDetail>>

  fun updateOrganisationLogo(
    orgId: String,
    imageUrl: String
  ): Flow<Result<Boolean>>

  fun updateOrganisationDescription(
    orgId: String,
    description: String
  ): Flow<Result<Boolean>>

  fun addMemberToOrganisation(
    memberId: String,
    organisationId: String,
    post: String,
    priority: Int = 0
  ): Flow<Result<Boolean>>

  fun removeMemberFromOrganisation(organisationalMemberId: String): Flow<Result<Boolean>>
  fun updateMemberPost(organisationalMemberId: String, post: String): Flow<Result<Boolean>>
  fun updateMemberPriority(organisationalMemberId: String, priority: Int): Flow<Result<Boolean>>
}

/**
 * Implementation of OrganisationsRepository that uses Apollo GraphQL client
 */
class OrganisationsRepositoryImpl(private val apolloClient: ApolloClient) : OrganisationsRepository {
  override fun getOrganisations(): Flow<Result<List<OrganisationWithDescription>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationsQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.organisationCollection?.edges?.map {
            OrganisationWithDescription(id = it.node.id, name = it.node.name!!, description = it.node.description!!)
          } ?: emptyList()
        }

      emit(result)
    }

  override fun getOrganisationById(id: String): Flow<Result<OrganisationDetail>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response =
            apolloClient.query(
              OrganisationQuery(
                filter =
                  Optional.present(
                    OrganisationFilter(id = Optional.present(StringFilter(eq = Optional.present(id))))
                  )
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.organisationCollection?.edges?.map {
            val node = it.node
            OrganisationDetail(
              id = node.id,
              name = node.name!!,
              description = node.description!!,
              logo = node.logo,
              members =
                it.node.organisational_memberCollection?.edges?.map {
                  val (id, post, priority, member) = it.node
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
        }

      emit(result)
    }

  override fun updateOrganisationLogo(
    orgId: String,
    imageUrl: String
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              UpdateOrganisationLogoMutation(orgId, imageUrl)
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.updateorganisationCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }

  override fun updateOrganisationDescription(
    orgId: String,
    description: String
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              UpdateOrganisationDescriptionMutation(id = orgId, description = description)
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.updateorganisationCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }

  override fun addMemberToOrganisation(
    memberId: String,
    organisationId: String,
    post: String,
    priority: Int
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result = safeCall {
        val response = apolloClient.mutation(
          AddMemberToOrganisationMutation(
            memberId = memberId,
            organisationId = organisationId,
            post = post,
            priority = priority
          )
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        response.data?.insertIntoorganisational_memberCollection?.affectedCount!! > 0
      }
      emit(result)
    }
  }

  override fun removeMemberFromOrganisation(organisationalMemberId: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result = safeCall {
        val response = apolloClient.mutation(
          RemoveMemberFromOrganisationMutation(organisationalMemberId = organisationalMemberId)
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        response.data?.deleteFromorganisational_memberCollection?.affectedCount!! > 0
      }
      emit(result)
    }
  }

  override fun updateMemberPost(organisationalMemberId: String, post: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result = safeCall {
        val response = apolloClient.mutation(
          UpdateOrganisationalMemberPostMutation(
            organisationalMemberId = organisationalMemberId,
            post = post
          )
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        response.data?.updateorganisational_memberCollection?.affectedCount!! > 0
      }
      emit(result)
    }
  }

  override fun updateMemberPriority(organisationalMemberId: String, priority: Int): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result = safeCall {
        val response = apolloClient.mutation(
          UpdateOrganisationalMemberPriorityMutation(
            organisationalMemberId = organisationalMemberId,
            priority = priority
          )
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        response.data?.updateorganisational_memberCollection?.affectedCount!! > 0
      }
      emit(result)
    }
  }
}

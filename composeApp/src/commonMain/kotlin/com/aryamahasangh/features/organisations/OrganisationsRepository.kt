package com.aryamahasangh.features.organisations

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.cacheInfo
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.apollographql.apollo.exception.CacheMissException
import com.aryamahasangh.*
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.network.supabaseClient
import com.aryamahasangh.type.OrganisationFilter
import com.aryamahasangh.type.OrganisationalMemberInsertInput
import com.aryamahasangh.type.StringFilter
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

  fun updateMemberPost(
    organisationalMemberId: String,
    post: String
  ): Flow<Result<Boolean>>

  fun updateMemberPriority(
    organisationalMemberId: String,
    priority: Int
  ): Flow<Result<Boolean>>

  fun updateMemberPriorities(memberPriorities: List<Pair<String, Int>>): Flow<Result<Boolean>>

  /**
   * Create a new organisation
   */
  fun createOrganisation(
    name: String,
    description: String,
    logoUrl: String,
    priority: Int,
    members: List<Triple<Member, String, Int>> // Member to Post pairs
  ): Flow<Result<String>> // Returns organisation ID

  /**
   * Delete an organisation by ID
   */
  fun deleteOrganisation(organisationId: String): Flow<Result<Boolean>>
}

/**
 * Implementation of OrganisationsRepository that uses Apollo GraphQL client
 */
class OrganisationsRepositoryImpl(private val apolloClient: ApolloClient) : OrganisationsRepository {
  override fun getOrganisations(): Flow<Result<List<OrganisationWithDescription>>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(OrganisationsQuery())
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val isCacheMissWithEmptyData = response.exception is CacheMissException &&
            response.data?.organisationCollection?.edges.isNullOrEmpty()

          if (isCacheMissWithEmptyData) {
            return@collect
          }

          //val cameFromEmptyCache =
          //  response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if (response.hasErrors()) {
              throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
            }
            response.data?.organisationCollection?.edges?.map {
              OrganisationWithDescription(id = it.node.id, name = it.node.name!!, logo = it.node.logo, description = it.node.description!!)
            } ?: emptyList()
          }
          emit(result)
        }
    }

  override fun getOrganisationById(id: String): Flow<Result<OrganisationDetail>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(
        OrganisationQuery(
          filter =
            Optional.present(
              OrganisationFilter(id = Optional.present(StringFilter(eq = Optional.present(id))))
            )
        )
      )
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if(!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
              }
            }
            val organisations = response.data?.organisationCollection?.edges?.map {
              val node = it.node
              OrganisationDetail(
                id = node.id,
                name = node.name!!,
                description = node.description!!,
                logo = node.logo,
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
                  }!!
              )
            }
            if (organisations.isNullOrEmpty() && !cameFromEmptyCache) {
              throw Exception("Organisation not found")
            }
            organisations?.firstOrNull() ?: throw Exception("Organisation not found")
          }
          if (!cameFromEmptyCache || (result is Result.Success)) {
            emit(result)
          }
        }
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
          response.data?.updateOrganisationCollection?.affectedCount!! > 0
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
          response.data?.updateOrganisationCollection?.affectedCount!! > 0
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
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
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
          response.data?.insertIntoOrganisationalMemberCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }

  override fun removeMemberFromOrganisation(organisationalMemberId: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              RemoveMemberFromOrganisationMutation(organisationalMemberId = organisationalMemberId)
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.deleteFromOrganisationalMemberCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }

  override fun updateMemberPost(
    organisationalMemberId: String,
    post: String
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              UpdateOrganisationalMemberPostMutation(
                organisationalMemberId = organisationalMemberId,
                post = post
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.updateOrganisationalMemberCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }

  override fun updateMemberPriority(
    organisationalMemberId: String,
    priority: Int
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              UpdateOrganisationalMemberPriorityMutation(
                organisationalMemberId = organisationalMemberId,
                priority = priority
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.updateOrganisationalMemberCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }

  /**
   * Updates the priorities for a list of organizational members.
   *
   * @param memberPriorities A list of pairs, where each pair contains a organisationalMember's id(OrganisationalMember::id) as a String and its priority as an Int.
   * @return A Flow object emitting a Result that encapsulates a Boolean indicating whether the updates were successful.
   */
  override fun updateMemberPriorities(memberPriorities: List<Pair<String, Int>>): Flow<Result<Boolean>> {
    println(memberPriorities)
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          kotlinx.coroutines.coroutineScope {
            val updateTasks =
              memberPriorities.map { (id, priority) ->
                async {
                  try {
                    supabaseClient.from("organisational_member").update(
                      {
                        set("priority", priority)
                      }
                    ) {
                      filter {
                        eq("id", id)
                      }
                    }
                  } catch (e: Exception) {
                    println(e)
                  }
                }
              }
            updateTasks.awaitAll()
            true // Return success if all updates complete without exception
          }
        }
      emit(result)
    }
  }

  override fun createOrganisation(
    name: String,
    description: String,
    logoUrl: String,
    priority: Int,
    members: List<Triple<Member, String, Int>> // Member to Post pairs
  ): Flow<Result<String>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              CreateOrganisationMutation(name, logoUrl, description, priority)
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          if (response.data?.insertIntoOrganisationCollection?.affectedCount!! > 0) {
            val organisationId = response.data?.insertIntoOrganisationCollection?.records?.first()?.id!!

            // Only add members if there are any
            if (members.isNotEmpty()) {
              val organisationalMembers =
                members.map { (member, post, memberPriority) ->
                  OrganisationalMemberInsertInput(
                    memberId = Optional.present(member.id),
                    organisationId = Optional.present(organisationId),
                    post = Optional.present(post),
                    priority = Optional.present(memberPriority)
                  )
                }

              val memberInsertResult =
                apolloClient.mutation(AddMembersToOrganisationMutation(organisationalMembers)).execute()
              if (memberInsertResult.hasErrors()) {
                throw Exception(
                  memberInsertResult.errors?.firstOrNull()?.message ?: "Error adding members to organisation"
                )
              }

              // Return organisation ID if members were added successfully
              organisationId
            } else {
              // Organisation created successfully without members
              organisationId
            }
          } else {
            // Organisation creation failed
            throw Exception("Failed to create organisation")
          }
        }
      emit(result)
    }
  }

  override fun deleteOrganisation(organisationId: String): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.mutation(RemoveOrganisationMutation(organisationId)).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.deleteFromOrganisationCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }
}

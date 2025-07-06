package com.aryamahasangh.features.admin.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.*
import com.aryamahasangh.*
import com.aryamahasangh.components.AddressData
import com.aryamahasangh.components.getActiveImageUrls
import com.aryamahasangh.features.activities.LatLng
import com.aryamahasangh.features.admin.PaginatedRepository
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.fragment.AryaSamajWithAddress
import com.aryamahasangh.type.*
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AryaSamajRepository : PaginatedRepository<AryaSamajWithAddress> {
  suspend fun getAryaSamajs(): Flow<Result<List<AryaSamajListItem>>>

  suspend fun getAryaSamajDetail(id: String): Flow<Result<AryaSamajDetail>>

  suspend fun createAryaSamaj(formData: AryaSamajFormData): Flow<Result<String>>

  suspend fun deleteAryaSamaj(id: String): Flow<Result<Boolean>>

  suspend fun updateAryaSamaj(
    id: String,
    formData: AryaSamajFormData
  ): Flow<Result<Boolean>>

  suspend fun getAryaSamajCount(): Flow<Result<Int>>

  suspend fun getAryaSamajByAddress(
    state: String?,
    district: String?,
    vidhansabha: String?
  ): Flow<Result<List<AryaSamajListItem>>>
}

class AryaSamajRepositoryImpl(private val apolloClient: ApolloClient) : AryaSamajRepository {
  override suspend fun getAryaSamajs(): Flow<Result<List<AryaSamajListItem>>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(AryaSamajsQuery())
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
            response.data?.aryaSamajCollection?.edges?.map { edge ->
              val aryaSamaj = edge.node
              val address = aryaSamaj.address?.addressFields
              val formattedAddress =
                if (address != null) {
                  buildString {
                    if (!address.basicAddress.isNullOrBlank()) append(address.basicAddress)
                    if (!address.district.isNullOrBlank()) {
                      if (isNotBlank()) append(", ")
                      append(address.district)
                    }
                    if (!address.state.isNullOrBlank()) {
                      if (isNotBlank()) append(", ")
                      append(address.state)
                    }
                  }
                } else {
                  ""
                }
              val aryaSamajFields = aryaSamaj.aryaSamajFields
              AryaSamajListItem(
                id = aryaSamajFields.id,
                name = aryaSamajFields.name ?: "",
                description = aryaSamajFields.description ?: "",
                formattedAddress = formattedAddress,
                memberCount = 0,
                mediaUrls = aryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()
              )
            } ?: emptyList()
          }
          emit(result)
        }
    }

  override suspend fun getAryaSamajDetail(id: String): Flow<Result<AryaSamajDetail>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(AryaSamajDetailQuery(aryaSamajId = id))
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if (!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
              }
            }

            val aryaSamajNode = response.data?.aryaSamajCollection?.edges?.firstOrNull()?.node
            if (aryaSamajNode == null && !cameFromEmptyCache) {
              throw Exception("Arya Samaj not found")
            }

            if (aryaSamajNode == null) {
              throw Exception("Arya Samaj not found")
            }

            val address = aryaSamajNode.address?.addressFields
            val addressData =
              if (address != null) {
                AddressData(
                  location =
                    if (address.latitude != null && address.longitude != null) {
                      LatLng(address.latitude, address.longitude)
                    } else {
                      null
                    },
                  address = address.basicAddress ?: "",
                  state = address.state ?: "",
                  district = address.district ?: "",
                  vidhansabha = address.vidhansabha ?: "",
                  pincode = address.pincode ?: ""
                )
              } else {
                AddressData()
              }

            val members =
              aryaSamajNode.samajMemberCollection?.edges?.mapNotNull { edge ->
                val samajMember = edge.node
                val member = samajMember.member
                if (member != null) {
                  AryaSamajMember(
                    id = samajMember.id,
                    memberId = member.id,
                    memberName = member.name ?: "",
                    memberProfileImage = member.profileImage,
                    memberPhoneNumber = member.phoneNumber,
                    post = samajMember.post ?: "",
                    priority = samajMember.priority ?: 0
                  )
                } else {
                  null
                }
              } ?: emptyList()
            val aryaSamaj = aryaSamajNode.aryaSamajFields
            AryaSamajDetail(
              id = aryaSamaj.id,
              name = aryaSamaj.name ?: "",
              description = aryaSamaj.description ?: "",
              mediaUrls = aryaSamaj.mediaUrls?.filterNotNull() ?: emptyList(),
              address = addressData,
              members = members,
              createdAt = aryaSamaj.createdAt?.toString()
            )
          }
          if (!cameFromEmptyCache || (result is Result.Success)) {
            emit(result)
          }
        }
    }

  override suspend fun createAryaSamaj(formData: AryaSamajFormData): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          // Step 1: Create address
          val addressResponse =
            apolloClient.mutation(
              CreateAddressMutation(
                basicAddress = formData.addressData.address,
                state = formData.addressData.state,
                district = formData.addressData.district,
                pincode = formData.addressData.pincode,
                latitude = Optional.presentIfNotNull(formData.addressData.location?.latitude),
                longitude = Optional.presentIfNotNull(formData.addressData.location?.longitude),
                vidhansabha = Optional.presentIfNotNull(formData.addressData.vidhansabha.ifBlank { null })
              )
            ).execute()

          if (addressResponse.hasErrors()) {
            throw Exception(addressResponse.errors?.firstOrNull()?.message ?: "Failed to create address")
          }

          val addressId =
            addressResponse.data?.insertIntoAddressCollection?.records?.firstOrNull()?.id
              ?: throw Exception("Failed to get address ID")

          // Step 2: Create Arya Samaj
          val mediaUrls = formData.imagePickerState.getActiveImageUrls()

          val aryaSamajResponse =
            apolloClient.mutation(
              CreateAryaSamajMutation(
                name = formData.name,
                description = formData.description,
                mediaUrls = Optional.presentIfNotNull(mediaUrls.ifEmpty { null }),
                addressId = Optional.presentIfNotNull(addressId)
              )
            ).execute()

          if (aryaSamajResponse.hasErrors()) {
            throw Exception(aryaSamajResponse.errors?.firstOrNull()?.message ?: "Failed to create Arya Samaj")
          }

          val aryaSamajId =
            aryaSamajResponse.data?.insertIntoAryaSamajCollection?.records?.firstOrNull()?.id
              ?: throw Exception("Failed to get Arya Samaj ID")

          // Step 3: Add members to Arya Samaj if any
          if (formData.membersState.hasMembers) {
            val samajMemberInputs =
              formData.membersState.members.map { (member, postPriority) ->
                SamajMemberInsertInput(
                  memberId = Optional.present(member.id),
                  aryaSamajId = Optional.present(aryaSamajId),
                  post = Optional.presentIfNotNull(postPriority.first),
                  priority = Optional.presentIfNotNull(postPriority.second)
                )
              }

            val membersResponse =
              apolloClient.mutation(
                AddMembersToAryaSamajMutation(input = samajMemberInputs)
              ).execute()

            if (membersResponse.hasErrors()) {
              throw Exception(membersResponse.errors?.firstOrNull()?.message ?: "Failed to add members to Arya Samaj")
            }
          }
          apolloClient.apolloStore.clearAll()
          aryaSamajId
        }
      emit(result)
    }

  override suspend fun deleteAryaSamaj(id: String): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.mutation(DeleteAryaSamajMutation(id)).execute()
          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            val userFriendlyMessage = when {
              errorMessage.contains("foreign key", ignoreCase = true) ||
                errorMessage.contains("constraint", ignoreCase = true) -> {
                "इस आर्य समाज को नहीं हटाया जा सकता क्योंकि यह अन्य रिकॉर्ड से जुड़ा हुआ है। कृपया पहले संबंधित रिकॉर्ड हटाएं।"
              }

              errorMessage.contains("violation", ignoreCase = true) -> {
                "डेटाबेस नियम उल्लंघन: $errorMessage"
              }

              errorMessage.contains("still referenced", ignoreCase = true) -> {
                "इस आर्य समाज से जुड़े सदस्य हैं। कृपया पहले सदस्यों को अन्य आर्य समाज में स्थानांतरित करें।"
              }

              else -> "आर्य समाज हटाने में त्रुटि: $errorMessage"
            }
            throw Exception(userFriendlyMessage)
          }

          val success = response.data?.deleteFromAryaSamajCollection?.affectedCount?.let { it > 0 } ?: false
          if (!success) {
            throw Exception("आर्य समाज नहीं मिला या पहले से ही हटा दिया गया है")
          }

          apolloClient.apolloStore.clearAll()

          success
        }
      emit(result)
    }

  override suspend fun updateAryaSamaj(
    id: String,
    formData: AryaSamajFormData
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          // Step 1: Get current AryaSamaj data to compare changes
          val currentDetailResponse = apolloClient.query(AryaSamajDetailQuery(aryaSamajId = id)).execute()
          if (currentDetailResponse.hasErrors()) {
            throw Exception(currentDetailResponse.errors?.firstOrNull()?.message ?: "Failed to fetch current data")
          }

          val currentAryaSamaj =
            currentDetailResponse.data?.aryaSamajCollection?.edges?.firstOrNull()?.node
              ?: throw Exception("AryaSamaj not found")

          val currentAddress = currentAryaSamaj.address?.addressFields
          val currentMembers = currentAryaSamaj.samajMemberCollection?.edges?.map { it.node } ?: emptyList()

          // Step 2: Update address if changed
          var addressId = currentAddress?.id
          val newAddressData = formData.addressData

          if (currentAddress != null) {
            val addressChanged =
              currentAddress.basicAddress != newAddressData.address ||
                currentAddress.state != newAddressData.state ||
                currentAddress.district != newAddressData.district ||
                currentAddress.pincode != newAddressData.pincode ||
                currentAddress.latitude != newAddressData.location?.latitude ||
                currentAddress.longitude != newAddressData.location?.longitude ||
                currentAddress.vidhansabha != newAddressData.vidhansabha

            if (addressChanged) {
              val updateAddressResponse =
                apolloClient.mutation(
                  UpdateAddressMutation(
                    id = currentAddress.id,
                    basicAddress = Optional.presentIfNotNull(newAddressData.address),
                    state = Optional.presentIfNotNull(newAddressData.state),
                    district = Optional.presentIfNotNull(newAddressData.district),
                    pincode = Optional.presentIfNotNull(newAddressData.pincode),
                    latitude = Optional.presentIfNotNull(newAddressData.location?.latitude),
                    longitude = Optional.presentIfNotNull(newAddressData.location?.longitude),
                    vidhansabha = Optional.presentIfNotNull(newAddressData.vidhansabha.ifBlank { null })
                  )
                ).execute()

              if (updateAddressResponse.hasErrors()) {
                throw Exception(updateAddressResponse.errors?.firstOrNull()?.message ?: "Failed to update address")
              }

              addressId = currentAddress.id
            }
          } else if (newAddressData.address.isNotBlank() || newAddressData.state.isNotBlank()) {
            // Create new address if it doesn't exist but we have address data
            val createAddressResponse =
              apolloClient.mutation(
                CreateAddressMutation(
                  basicAddress = newAddressData.address,
                  state = newAddressData.state,
                  district = newAddressData.district,
                  pincode = newAddressData.pincode,
                  latitude = Optional.presentIfNotNull(newAddressData.location?.latitude),
                  longitude = Optional.presentIfNotNull(newAddressData.location?.longitude),
                  vidhansabha = Optional.presentIfNotNull(newAddressData.vidhansabha.ifBlank { null })
                )
              ).execute()

            if (createAddressResponse.hasErrors()) {
              throw Exception(createAddressResponse.errors?.firstOrNull()?.message ?: "Failed to create address")
            }

            addressId =
              createAddressResponse.data?.insertIntoAddressCollection?.records?.firstOrNull()?.id
          }

          // Step 3: Update main AryaSamaj data
          val currentAryaSamajFields = currentAryaSamaj.aryaSamajFields
          val mediaUrls = formData.imagePickerState.getActiveImageUrls()

          val aryaSamajChanged =
            currentAryaSamajFields.name != formData.name ||
              currentAryaSamajFields.description != formData.description ||
              (currentAryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()) != mediaUrls

          if (aryaSamajChanged) {
            val updateAryaSamajResponse =
              apolloClient.mutation(
                UpdateAryaSamajMutation(
                  id = id,
                  name = Optional.presentIfNotNull(formData.name),
                  description = Optional.presentIfNotNull(formData.description),
                  mediaUrls = Optional.presentIfNotNull(mediaUrls.ifEmpty { null }),
                  addressId = Optional.presentIfNotNull(addressId)
                )
              ).execute()

            if (updateAryaSamajResponse.hasErrors()) {
              throw Exception(updateAryaSamajResponse.errors?.firstOrNull()?.message ?: "Failed to update AryaSamaj")
            }
          }

          // Step 4: Handle member changes
          val newMembers = formData.membersState.members

          // Create maps for easier comparison
          val currentMemberMap = currentMembers.filter { it.member != null }.associateBy { it.member!!.id }
          val newMemberMap: Map<String, Pair<String, Int>> =
            newMembers.map { (member, postPriority) ->
              member.id to postPriority
            }.toMap()

          // Remove members that are no longer present
          currentMembers.forEach { currentMember ->
            val currentMemberId = currentMember.member?.id
            if (currentMemberId != null && !newMemberMap.containsKey(currentMemberId)) {
              val removeMemberResponse =
                apolloClient.mutation(
                  RemoveMemberFromAryaSamajMutation(samajMemberId = currentMember.id)
                ).execute()

              if (removeMemberResponse.hasErrors()) {
                throw Exception(removeMemberResponse.errors?.firstOrNull()?.message ?: "Failed to remove member")
              }
            }
          }

          // Add new members and update existing ones
          newMembers.forEach { (member, postPriority) ->
            val (post, priority) = postPriority
            val currentMember = currentMemberMap[member.id]

            if (currentMember == null) {
              // Add new member
              val addMemberResponse =
                apolloClient.mutation(
                  AddMemberToAryaSamajMutation(
                    memberId = member.id,
                    aryaSamajId = id,
                    post = Optional.presentIfNotNull(post),
                    priority = Optional.presentIfNotNull(priority)
                  )
                ).execute()

              if (addMemberResponse.hasErrors()) {
                throw Exception(addMemberResponse.errors?.firstOrNull()?.message ?: "Failed to add member")
              }
            } else {
              // Update existing member if changed
              var memberChanged = false

              if (currentMember.post != post) {
                val updatePostResponse =
                  apolloClient.mutation(
                    UpdateSamajMemberPostMutation(
                      samajMemberId = currentMember.id,
                      post = post
                    )
                  ).execute()

                if (updatePostResponse.hasErrors()) {
                  throw Exception(updatePostResponse.errors?.firstOrNull()?.message ?: "Failed to update member post")
                }
                memberChanged = true
              }

              if (currentMember.priority != priority) {
                val updatePriorityResponse =
                  apolloClient.mutation(
                    UpdateSamajMemberPriorityMutation(
                      samajMemberId = currentMember.id,
                      priority = priority
                    )
                  ).execute()

                if (updatePriorityResponse.hasErrors()) {
                  throw Exception(
                    updatePriorityResponse.errors?.firstOrNull()?.message ?: "Failed to update member priority"
                  )
                }
                memberChanged = true
              }
            }
          }

          true
        }
      emit(result)
    }

  override suspend fun getAryaSamajCount(): Flow<Result<Int>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(AryaSamajCountQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.aryaSamajCollection?.totalCount ?: 0
        }
      emit(result)
    }

  override suspend fun getAryaSamajByAddress(
    state: String?,
    district: String?,
    vidhansabha: String?
  ): Flow<Result<List<AryaSamajListItem>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          // First, find addresses that match the criteria
          val addressFilters = mutableListOf<AddressFilter>()

          if (!state.isNullOrBlank()) {
            addressFilters.add(AddressFilter(state = Optional.present(StringFilter(eq = Optional.present(state)))))
          }
          if (!district.isNullOrBlank()) {
            addressFilters.add(AddressFilter(district = Optional.present(StringFilter(eq = Optional.present(district)))))
          }
          if (!vidhansabha.isNullOrBlank()) {
            val vidhansabhaStringFilter = StringFilter(eq = Optional.present(vidhansabha))
            addressFilters.add(
              AddressFilter(
                vidhansabha = Optional.present(
                  vidhansabhaStringFilter
                )
              )
            )
          }

          if (addressFilters.isEmpty()) {
            // If no filters, return all AryaSamaj
            val response = apolloClient.query(AryaSamajsQuery()).execute()
            if (response.hasErrors()) {
              throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
            }
            return@safeCall response.data?.aryaSamajCollection?.edges?.map { edge ->
              val aryaSamaj = edge.node
              val address = aryaSamaj.address?.addressFields
              val formattedAddress =
                if (address != null) {
                  buildString {
                    if (!address.basicAddress.isNullOrBlank()) append(address.basicAddress)
                    if (!address.district.isNullOrBlank()) {
                      if (isNotBlank()) append(", ")
                      append(address.district)
                    }
                    if (!address.state.isNullOrBlank()) {
                      if (isNotBlank()) append(", ")
                      append(address.state)
                    }
                  }
                } else {
                  ""
                }
              val aryaSamajFields = aryaSamaj.aryaSamajFields
              AryaSamajListItem(
                id = aryaSamajFields.id,
                name = aryaSamajFields.name ?: "",
                description = aryaSamajFields.description ?: "",
                formattedAddress = formattedAddress,
                memberCount = 0,
                mediaUrls = aryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()
              )
            } ?: emptyList()
          }

          // Get addresses that match criteria
          val addressResponse = apolloClient.query(
            AddressesQuery(
              filter = Optional.present(
                AddressFilter(
                  and = Optional.present(addressFilters)
                )
              )
            )
          ).execute()

          if (addressResponse.hasErrors()) {
            throw Exception(addressResponse.errors?.firstOrNull()?.message ?: "Failed to get addresses")
          }

          val matchingAddressIds =
            addressResponse.data?.addressCollection?.edges?.map { it.node.addressFields.id } ?: emptyList()

          if (matchingAddressIds.isEmpty()) {
            return@safeCall emptyList<AryaSamajListItem>()
          }

          // Now get AryaSamaj with those addressIds
          val aryaSamajResponse = apolloClient.query(
            AryaSamajsQuery(
              filter = Optional.present(
                AryaSamajFilter(
                  addressId = Optional.present(
                    StringFilter(
                      `in` = Optional.present(matchingAddressIds)
                    )
                  )
                )
              )
            )
          ).execute()

          if (aryaSamajResponse.hasErrors()) {
            throw Exception(aryaSamajResponse.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          aryaSamajResponse.data?.aryaSamajCollection?.edges?.map { edge ->
            val aryaSamaj = edge.node
            val address = aryaSamaj.address?.addressFields
            val formattedAddress =
              if (address != null) {
                buildString {
                  if (!address.basicAddress.isNullOrBlank()) append(address.basicAddress)
                  if (!address.district.isNullOrBlank()) {
                    if (isNotBlank()) append(", ")
                    append(address.district)
                  }
                  if (!address.state.isNullOrBlank()) {
                    if (isNotBlank()) append(", ")
                    append(address.state)
                  }
                }
              } else {
                ""
              }
            val aryaSamajFields = aryaSamaj.aryaSamajFields
            AryaSamajListItem(
              id = aryaSamajFields.id,
              name = aryaSamajFields.name ?: "",
              description = aryaSamajFields.description ?: "",
              formattedAddress = formattedAddress,
              memberCount = 0,
              mediaUrls = aryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun getItemsPaginated(
    pageSize: Int,
    cursor: String?,
    filter: Any?
  ): Flow<PaginationResult<AryaSamajWithAddress>> =
    flow {
      emit(PaginationResult.Loading())

      apolloClient.query(
        GetAryaSamajsQuery(
          first = pageSize,
          after = Optional.presentIfNotNull(cursor),
          filter = Optional.presentIfNotNull(filter as? AryaSamajFilter),
          orderBy = Optional.present(listOf(AryaSamajOrderBy(createdAt = Optional.present(OrderByDirection.DescNullsLast))))
        )
      )
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          // Skip only cache MISSES with empty data - cache HITS with empty data should be shown
          if (response.isFromCache && response.cacheInfo?.isCacheHit == false && response.data?.aryaSamajCollection?.edges.isNullOrEmpty()) {
            // Skip emitting empty cache miss - wait for network
          } else {
            val result = safeCall {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
              }

              val aryaSamajs = response.data?.aryaSamajCollection?.edges?.map {
                it.node.aryaSamajWithAddress
              } ?: emptyList()

              val pageInfo = response.data?.aryaSamajCollection?.pageInfo

              PaginationResult.Success(
                data = aryaSamajs,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                endCursor = pageInfo?.endCursor
              )
            }

            when (result) {
              is Result.Success -> {
                emit(result.data)
              }

              is Result.Error -> {
                emit(PaginationResult.Error(result.exception?.message ?: "Unknown error"))
              }

              is Result.Loading -> {
                // Already emitted loading state above
              }
            }
          }
        }
    }

  override suspend fun searchItemsPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String?
  ): Flow<PaginationResult<AryaSamajWithAddress>> =
    flow {
      emit(PaginationResult.Loading())
      val result =
        safeCall {
          val response =
            apolloClient.query(
              SearchAryaSamajsQuery(
                searchTerm = "%$searchTerm%",
                first = pageSize,
                after = Optional.presentIfNotNull(cursor)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          val aryaSamajs = response.data?.aryaSamajCollection?.edges?.map {
            it.node.aryaSamajWithAddress
          } ?: emptyList()
          val hasNextPage = response.data?.aryaSamajCollection?.pageInfo?.hasNextPage ?: false
          val endCursor = response.data?.aryaSamajCollection?.pageInfo?.endCursor

          PaginationResult.Success(data = aryaSamajs, hasNextPage = hasNextPage, endCursor = endCursor)
        }

      when (result) {
        is Result.Success -> emit(result.data)
        is Result.Error -> emit(PaginationResult.Error(result.message))
        is Result.Loading -> {} // Already emitted Loading above
      }
    }
}

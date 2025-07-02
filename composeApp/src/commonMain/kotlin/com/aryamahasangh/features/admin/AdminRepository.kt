
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.cacheInfo
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.aryamahasangh.*
import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.components.Gender
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.admin.*
import com.aryamahasangh.fragment.AryaSamajFields
import com.aryamahasangh.fragment.MemberInOrganisationShort
import com.aryamahasangh.network.supabaseClient
import com.aryamahasangh.type.GenderFilter
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

// Simple data classes for realtime change tracking
@kotlinx.serialization.Serializable
data class AdminTableChange(
  val id: String
)

// Data class for consolidated admin counts
data class AdminCounts(
  val organisationalMembersCount: Long = 0L,
  val aryaSamajCount: Long = 0L,
  val familyCount: Long = 0L,
  val ekalAryaCount: Long = 0L
)

interface AdminRepository {
  // NEW: Consolidated counts method
  suspend fun getAdminCounts(): Flow<Result<AdminCounts>>

  // NEW: Real-time listener for admin count changes
  fun listenToAdminCountChanges(): Flow<Unit>

  // Member methods
  suspend fun getOrganisationalMembers(): Flow<Result<List<MemberShort>>>

  suspend fun getMembersCount(): Flow<Result<Long>>

  suspend fun getMember(id: String): Flow<Result<MemberDetail>>

  suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>>

  suspend fun searchOrganisationalMembers(query: String): Flow<Result<List<MemberInOrganisationShort>>>

  // Search members that returns Member list for MembersComponent
  suspend fun searchMembersForSelection(query: String): Flow<Result<List<Member>>>

  // EkalArya (Member not in family) methods
  suspend fun getEkalAryaMembers(): Flow<Result<List<MemberShort>>>

  suspend fun searchEkalAryaMembers(query: String): Flow<Result<List<MemberShort>>>

  // NEW: Paginated EkalArya methods
  suspend fun getEkalAryaMembersPaginated(
    pageSize: Int = 30,
    cursor: String? = null
  ): Flow<PaginationResult<MemberShort>>

  suspend fun searchEkalAryaMembersPaginated(
    searchTerm: String,
    pageSize: Int = 30,
    cursor: String? = null
  ): Flow<PaginationResult<MemberShort>>

  suspend fun updateMemberDetails(
    memberId: String,
    name: String?,
    phoneNumber: String?,
    educationalQualification: String?,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImage: String?,
    addressId: String?,
    tempAddressId: String?,
    referrerId: String?,
    aryaSamajId: String?,
    // Main address fields
    basicAddress: String?,
    state: String?,
    district: String?,
    pincode: String?,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?,
    // Temp address fields 
    tempBasicAddress: String?,
    tempState: String?,
    tempDistrict: String?,
    tempPincode: String?,
    tempLatitude: Double?,
    tempLongitude: Double?,
    tempVidhansabha: String?
  ): Flow<Result<Boolean>>

  suspend fun updateMemberPhoto(
    memberId: String,
    photoUrl: String
  ): Flow<Result<Boolean>>

  // AryaSamaj methods
  suspend fun getAllAryaSamajs(): Flow<Result<List<AryaSamaj>>>

  suspend fun searchAryaSamajs(query: String): Flow<Result<List<AryaSamaj>>>
  // New create address method
  suspend fun createAddress(
    basicAddress: String,
    state: String,
    district: String,
    pincode: String,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?
  ): Flow<Result<String>>

  suspend fun deleteMember(memberId: String): Flow<Result<Boolean>>

  // Comprehensive create member method with inline address creation
  suspend fun createMemberWithAddress(
    name: String,
    phoneNumber: String,
    educationalQualification: String?,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImageUrl: String?,
    referrerId: String?,
    aryaSamajId: String?,
    // Main address fields
    basicAddress: String?,
    state: String?,
    district: String?,
    pincode: String?,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?,
    // Temp address fields 
    tempBasicAddress: String?,
    tempState: String?,
    tempDistrict: String?,
    tempPincode: String?,
    tempLatitude: Double?,
    tempLongitude: Double?,
    tempVidhansabha: String?
  ): Flow<Result<String>>
}

class AdminRepositoryImpl(private val apolloClient: ApolloClient) : AdminRepository {

  override suspend fun searchOrganisationalMembers(query: String): Flow<Result<List<MemberInOrganisationShort>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(SearchOrganisationalMembersQuery("%$query%")).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          if (response.data?.memberInOrganisationCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
          }
          response.data?.memberInOrganisationCollection?.edges?.map {
            val member = it.node.memberInOrganisationShort
            MemberInOrganisationShort(
              id = member.id,
              name = member.name!!,
              profileImage = member.profileImage ?: "",
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun getAdminCounts(): Flow<Result<AdminCounts>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(CountsForAdminContainerQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          val organisationalMembersCount = response.data?.memberInOrganisationCollection?.totalCount?.toLong() ?: 0L
          val aryaSamajCount = response.data?.aryaSamajCollection?.totalCount?.toLong() ?: 0L
          val familyCount = response.data?.familyCollection?.totalCount?.toLong() ?: 0L
          val ekalAryaCount = response.data?.memberNotInFamilyCollection?.totalCount?.toLong() ?: 0L
          AdminCounts(
            organisationalMembersCount = organisationalMembersCount,
            aryaSamajCount = aryaSamajCount,
            familyCount = familyCount,
            ekalAryaCount = ekalAryaCount
          )
        }
      emit(result)
    }

  override suspend fun getOrganisationalMembers(): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(MembersInOrganisationQuery())
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if(!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
              }
              if (response.data?.memberInOrganisationCollection?.edges.isNullOrEmpty()) {
                throw Exception("No results found")
              }
            }
            response.data?.memberInOrganisationCollection?.edges?.map {
              val member = it.node.memberInOrganisationShort
              MemberShort(
                id = member.id!!,
                name = member.name!!,
                profileImage = member.profileImage ?: "",
              )
            } ?: emptyList()
          }
          emit(result)
        }
    }

  override suspend fun getMembersCount(): Flow<Result<Long>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(MembersCountQuery()).execute()
//          var count = 0L
//          try {
//            count = supabaseClient.from("member_in_organisation").select { Count.EXACT }.countOrNull() ?: 0L
//          } catch (e: Exception) {
//            throw Exception("Unknown error occurred ${e.message}")
//          }

          response.data?.memberCollection?.totalCount?.toLong() ?: 0L
        }
      emit(result)
    }

  override suspend fun getMember(id: String): Flow<Result<MemberDetail>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(MemberDetailQuery(id))
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if (!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
              }
            }
            val memberNode = response.data?.memberCollection?.edges?.firstOrNull()?.node
            if (memberNode == null && !cameFromEmptyCache) {
              throw Exception("Member not found")
            }

            if (memberNode == null) {
              throw Exception("Member not found")
            }

            val organisations =
              memberNode.organisationalMemberCollection?.edges?.map {
                val organisation = it.node.organisation!!
                OrganisationInfo(
                  id = organisation.id,
                  name = organisation.name!!,
                  logo = organisation.logo ?: ""
                )
              }
            val activities =
              memberNode.activityMemberCollection?.edges?.map {
                val activity = it.node.activity!!
                ActivityInfo(
                  id = activity.id,
                  name = activity.name!!,
                  district = activity.district!!,
                  state = activity.state!!,
                  startDatetime = activity.startDatetime!!.toLocalDateTime(TimeZone.currentSystemDefault()),
                  endDatetime = activity.endDatetime!!.toLocalDateTime(TimeZone.currentSystemDefault())
                )
              }

            val samajPositions =
              memberNode.samajMemberCollection?.edges?.map { edge ->
                SamajPositionInfo(
                  id = edge.node.id,
                  post = edge.node.post ?: "",
                  priority = edge.node.priority ?: 0,
                  aryaSamaj = AryaSamajFields(
                    id = edge.node.aryaSamaj?.id ?: "",
                    name = edge.node.aryaSamaj?.name ?: "",
                    description = edge.node.aryaSamaj?.description ?: "",
                    mediaUrls = emptyList(),
                    addressId = null,
                    address = null,
                    createdAt = Clock.System.now()
                  )
                )
              } ?: emptyList()

            val aryaSamaj = memberNode.aryaSamaj?.aryaSamajFields

            val address = memberNode.address
            val addressInfo =
              if (address != null) {
                "${address.addressFields.basicAddress ?: ""}, ${address.addressFields.district ?: ""}, ${address.addressFields.state ?: ""} ${address.addressFields.pincode ?: ""}"
              } else {
                ""
              }

            val tempAddress = memberNode.tempAddress
            val tempAddressInfo =
              if (tempAddress != null) {
                "${tempAddress.addressFields.basicAddress ?: ""}, ${tempAddress.addressFields.district ?: ""}, ${tempAddress.addressFields.state ?: ""} ${tempAddress.addressFields.pincode ?: ""}"
              } else {
                ""
              }

            // Convert GenderFilter to Gender enum
            val gender = when (memberNode.gender) {
              GenderFilter.MALE -> Gender.MALE
              GenderFilter.FEMALE -> Gender.FEMALE
              GenderFilter.ANY -> Gender.ANY
              GenderFilter.UNKNOWN__ -> Gender.ANY
              null -> null
            }

            // Map referrer info
            val referrer = memberNode.referrer?.let {
              ReferrerInfo(
                id = it.id,
                name = it.name,
                profileImage = it.profileImage ?: ""
              )
            }

            MemberDetail(
              id = memberNode.id,
              name = memberNode.name ?: "",
              profileImage = memberNode.profileImage ?: "",
              phoneNumber = memberNode.phoneNumber ?: "",
              educationalQualification = memberNode.educationalQualification ?: "",
              email = memberNode.email ?: "",
              dob = memberNode.dob,
              joiningDate = memberNode.joiningDate,
              gender = gender,
              introduction = memberNode.introduction ?: "",
              occupation = memberNode.occupation ?: "",
              referrerId = memberNode.referrerId,
              referrer = referrer,
              address = addressInfo,
              addressFields = address?.addressFields,
              tempAddress = tempAddressInfo,
              tempAddressFields = tempAddress?.addressFields,
              district = address?.addressFields?.district ?: "",
              state = address?.addressFields?.state ?: "",
              pincode = address?.addressFields?.pincode ?: "",
              organisations = organisations ?: emptyList(),
              activities = activities ?: emptyList(),
              samajPositions = samajPositions,
              aryaSamaj = aryaSamaj
            )
          }
          if (!cameFromEmptyCache || (result is Result.Success)) {
            emit(result)
          }
        }
    }

  override suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(SearchMembersQuery("%$query%")).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          if (response.data?.memberCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
          }
          response.data?.memberCollection?.edges?.map {
            val member = it.node.memberShort
            MemberShort(
              id = member.id!!,
              name = member.name!!,
              profileImage = member.profileImage ?: "",
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun searchMembersForSelection(query: String): Flow<Result<List<Member>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(SearchMembersQuery("%$query%")).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          if (response.data?.memberCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
          }
          response.data?.memberCollection?.edges?.map { edge ->
            val member = edge.node.memberShort
            Member(
              id = member.id,
              name = member.name!!,
              profileImage = member.profileImage ?: ""
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun deleteMember(memberId: String): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.mutation(DeleteMemberMutation(memberId)).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }

          // Since DeleteMember returns JSON, we'll return true for success
          true
        }
      emit(result)
    }

  override suspend fun updateMemberDetails(
    memberId: String,
    name: String?,
    phoneNumber: String?,
    educationalQualification: String?,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImage: String?,
    addressId: String?,
    tempAddressId: String?,
    referrerId: String?,
    aryaSamajId: String?,
    // Main address fields
    basicAddress: String?,
    state: String?,
    district: String?,
    pincode: String?,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?,
    // Temp address fields 
    tempBasicAddress: String?,
    tempState: String?,
    tempDistrict: String?,
    tempPincode: String?,
    tempLatitude: Double?,
    tempLongitude: Double?,
    tempVidhansabha: String?
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.mutation(
            UpdateMemberDetailsComprehensiveMutation(
              memberId = memberId,
              name = Optional.presentIfNotNull(name),
              phoneNumber = Optional.presentIfNotNull(phoneNumber),
              educationalQualification = Optional.presentIfNotNull(educationalQualification),
              email = Optional.presentIfNotNull(email),
              dob = Optional.presentIfNotNull(dob),
              gender = Optional.presentIfNotNull(
                when (gender) {
                  Gender.MALE -> "MALE"
                  Gender.FEMALE -> "FEMALE"
                  Gender.ANY -> "ANY"
                  null -> null
                }
              ),
              occupation = Optional.presentIfNotNull(occupation),
              joiningDate = Optional.presentIfNotNull(joiningDate),
              introduction = Optional.presentIfNotNull(introduction),
              profileImage = Optional.presentIfNotNull(profileImage),
              addressId = Optional.presentIfNotNull(addressId),
              tempAddressId = Optional.presentIfNotNull(tempAddressId),
              referrerId = Optional.presentIfNotNull(referrerId),
              aryaSamajId = Optional.presentIfNotNull(aryaSamajId),
              // Main address fields
              basicAddress = Optional.presentIfNotNull(basicAddress),
              state = Optional.presentIfNotNull(state),
              district = Optional.presentIfNotNull(district),
              pincode = Optional.presentIfNotNull(pincode),
              latitude = Optional.presentIfNotNull(latitude),
              longitude = Optional.presentIfNotNull(longitude),
              vidhansabha = Optional.presentIfNotNull(vidhansabha),
              // Temp address fields
              tempBasicAddress = Optional.presentIfNotNull(tempBasicAddress),
              tempState = Optional.presentIfNotNull(tempState),
              tempDistrict = Optional.presentIfNotNull(tempDistrict),
              tempPincode = Optional.presentIfNotNull(tempPincode),
              tempLatitude = Optional.presentIfNotNull(tempLatitude),
              tempLongitude = Optional.presentIfNotNull(tempLongitude),
              tempVidhansabha = Optional.presentIfNotNull(tempVidhansabha)
            )
          ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Update failed")
          }
          true
        }
      emit(result)
    }

  override suspend fun updateMemberPhoto(
    memberId: String,
    photoUrl: String
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              UpdateMemberPhotoMutation(
                memberId = memberId,
                profileImageUrl = photoUrl
              )
            ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          true
        }
      emit(result)
    }

  override suspend fun createAddress(
    basicAddress: String,
    state: String,
    district: String,
    pincode: String,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?
  ): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              CreateAddressMutation(
                basicAddress = basicAddress,
                state = state,
                district = district,
                pincode = pincode,
                latitude = Optional.presentIfNotNull(latitude),
                longitude = Optional.presentIfNotNull(longitude),
                vidhansabha = Optional.presentIfNotNull(vidhansabha)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Address creation failed")
          }

          val addressId = response.data?.insertIntoAddressCollection?.records?.firstOrNull()?.id
          if (addressId == null) {
            throw Exception("Address creation failed - no ID returned")
          }

          addressId
        }
      emit(result)
    }

  override suspend fun getAllAryaSamajs(): Flow<Result<List<AryaSamaj>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(AryaSamajsQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          if (response.data?.aryaSamajCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
          }
          response.data?.aryaSamajCollection?.edges?.map { edge ->
            val node = edge.node
            val aryaSamajFields = node.aryaSamajFields
            val address = node.address
            AryaSamaj(
              id = aryaSamajFields.id,
              name = aryaSamajFields.name ?: "",
              address = "${address?.addressFields?.basicAddress ?: ""}, ${address?.addressFields?.district ?: ""}, ${address?.addressFields?.state ?: ""} ${address?.addressFields?.pincode ?: ""}".trim(),
              district = address?.addressFields?.district ?: ""
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun searchAryaSamajs(query: String): Flow<Result<List<AryaSamaj>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          // For now, just return all AryaSamajs and filter client-side
          // You can implement a proper search query later if needed
          val response = apolloClient.query(AryaSamajsQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Unknown error occurred")
          }
          if (response.data?.aryaSamajCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
          }
          val allAryaSamajs =
            response.data?.aryaSamajCollection?.edges?.map { edge ->
              val node = edge.node
              val aryaSamajFields = node.aryaSamajFields
              val address = node.address

              AryaSamaj(
                id = aryaSamajFields.id,
                name = aryaSamajFields.name ?: "",
                address = "${address?.addressFields?.basicAddress ?: ""}, ${address?.addressFields?.district ?: ""}, ${address?.addressFields?.state ?: ""} ${address?.addressFields?.pincode ?: ""}".trim(),
                district = address?.addressFields?.district ?: ""
              )
            } ?: emptyList()

          // Filter by query
          allAryaSamajs.filter { aryaSamaj ->
            aryaSamaj.name.contains(query, ignoreCase = true) ||
              aryaSamaj.address.contains(query, ignoreCase = true) ||
              aryaSamaj.district.contains(query, ignoreCase = true)
          }
        }
      emit(result)
    }

  override suspend fun getEkalAryaMembers(): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(EkalAryaMembersQuery(first = 1000, after = Optional.Absent))
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
              if (response.data?.memberNotInFamilyCollection?.edges.isNullOrEmpty()) {
                throw Exception("No results found")
              }
            }
            response.data?.memberNotInFamilyCollection?.edges?.map {
              val member = it.node.memberNotInFamilyShort
              MemberShort(
                id = member.id!!,
                name = member.name!!,
                profileImage = member.profileImage ?: "",
                place = ""
              )
            } ?: emptyList()
          }
          emit(result)
        }
    }

  override suspend fun searchEkalAryaMembers(query: String): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(
            SearchEkalAryaMembersQuery(
              first = 1000,
              after = Optional.Absent,
              searchTerm = "%$query%"
            )
          ).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          if (response.data?.memberNotInFamilyCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
          }
          response.data?.memberNotInFamilyCollection?.edges?.map {
            val member = it.node.memberNotInFamilyShort
            MemberShort(
              id = member.id!!,
              name = member.name!!,
              profileImage = member.profileImage ?: "",
              place = ""
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun getEkalAryaMembersPaginated(
    pageSize: Int,
    cursor: String?
  ): Flow<PaginationResult<MemberShort>> =
    flow {
      emit(PaginationResult.Loading())
      val result = safeCall {
        val response = apolloClient.query(
          EkalAryaMembersQuery(
            first = pageSize,
            after = Optional.presentIfNotNull(cursor)
          )
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        val members = response.data?.memberNotInFamilyCollection?.edges?.map {
          val member = it.node.memberNotInFamilyShort
          MemberShort(
            id = member.id!!,
            name = member.name!!,
            profileImage = member.profileImage ?: "",
            place = ""
          )
        } ?: emptyList()
        val pageInfo = response.data?.memberNotInFamilyCollection?.pageInfo
        PaginationResult.Success(
          data = members,
          hasNextPage = pageInfo?.hasNextPage ?: false,
          endCursor = pageInfo?.endCursor
        )
      }
      when (result) {
        is Result.Success -> emit(result.data)
        is Result.Error -> emit(PaginationResult.Error(result.exception?.message?.let { "$it" } ?: "Unknown error"))
        is Result.Loading -> emit(PaginationResult.Loading())
      }
    }

  override suspend fun searchEkalAryaMembersPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String?
  ): Flow<PaginationResult<MemberShort>> =
    flow {
      emit(PaginationResult.Loading())
      val result = safeCall {
        val response = apolloClient.query(
          SearchEkalAryaMembersQuery(
            first = pageSize,
            after = Optional.presentIfNotNull(cursor),
            searchTerm = "%$searchTerm%"
          )
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        val members = response.data?.memberNotInFamilyCollection?.edges?.map {
          val member = it.node.memberNotInFamilyShort
          MemberShort(
            id = member.id!!,
            name = member.name!!,
            profileImage = member.profileImage ?: "",
            place = ""
          )
        } ?: emptyList()
        val pageInfo = response.data?.memberNotInFamilyCollection?.pageInfo
        PaginationResult.Success(
          data = members,
          hasNextPage = pageInfo?.hasNextPage ?: false,
          endCursor = pageInfo?.endCursor
        )
      }
      when (result) {
        is Result.Success -> emit(result.data)
        is Result.Error -> emit(PaginationResult.Error(result.exception?.message?.let { "$it" } ?: "Unknown error"))
        is Result.Loading -> emit(PaginationResult.Loading())
      }
    }

  override suspend fun createMemberWithAddress(
    name: String,
    phoneNumber: String,
    educationalQualification: String?,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImageUrl: String?,
    referrerId: String?,
    aryaSamajId: String?,
    // Main address fields
    basicAddress: String?,
    state: String?,
    district: String?,
    pincode: String?,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?,
    // Temp address fields 
    tempBasicAddress: String?,
    tempState: String?,
    tempDistrict: String?,
    tempPincode: String?,
    tempLatitude: Double?,
    tempLongitude: Double?,
    tempVidhansabha: String?
  ): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              InsertMemberDetailsMutation(
                name = name,
                phoneNumber = phoneNumber,
                educationalQualification = Optional.presentIfNotNull(educationalQualification),
                email = Optional.presentIfNotNull(email),
                dob = Optional.presentIfNotNull(dob),
                gender = Optional.presentIfNotNull(
                  when (gender) {
                    Gender.MALE -> "MALE"
                    Gender.FEMALE -> "FEMALE"
                    Gender.ANY -> "OTHER"
                    null -> null
                  }
                ),
                occupation = Optional.presentIfNotNull(occupation),
                joiningDate = Optional.presentIfNotNull(joiningDate),
                introduction = Optional.presentIfNotNull(introduction),
                profileImage = Optional.presentIfNotNull(profileImageUrl),
                referrerId = Optional.presentIfNotNull(referrerId),
                aryaSamajId = Optional.presentIfNotNull(aryaSamajId),
                // Address parameters for automatic address creation
                addressId = Optional.presentIfNotNull(null), // Always null for new member creation
                basicAddress = Optional.presentIfNotNull(basicAddress),
                state = Optional.presentIfNotNull(state),
                district = Optional.presentIfNotNull(district),
                pincode = Optional.presentIfNotNull(pincode),
                latitude = Optional.presentIfNotNull(latitude),
                longitude = Optional.presentIfNotNull(longitude),
                vidhansabha = Optional.presentIfNotNull(vidhansabha),
                // Temp address parameters for automatic temp address creation
                tempAddressId = Optional.presentIfNotNull(null), // Always null for new member creation
                tempBasicAddress = Optional.presentIfNotNull(tempBasicAddress),
                tempState = Optional.presentIfNotNull(tempState),
                tempDistrict = Optional.presentIfNotNull(tempDistrict),
                tempPincode = Optional.presentIfNotNull(tempPincode),
                tempLatitude = Optional.presentIfNotNull(tempLatitude),
                tempLongitude = Optional.presentIfNotNull(tempLongitude),
                tempVidhansabha = Optional.presentIfNotNull(tempVidhansabha)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message?.let { "$it" } ?: "Member creation failed")
          }

          // Parse the JSON response from insertMemberDetails function
          val responseData = response.data?.insertMemberDetails
          if (responseData == null) {
            throw Exception("No response data received")
          }

          try {
            val jsonConfig = Json {
              ignoreUnknownKeys = true
              encodeDefaults = true
              isLenient = true
            }
            val jsonString = responseData.toString()
            // Parse using the same jsonConfig instance
            val jsonResponse =
              jsonConfig.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(jsonString)

            val successElement = jsonResponse["success"]?.toString()
            val success = successElement?.contains("true") == true

            if (success) {
              val memberIdElement = jsonResponse["member_id"]?.toString()
              val memberId =
                memberIdElement?.removeSurrounding("\"")?.removeSurrounding("JsonPrimitive(")?.removeSurrounding(")")
              memberId ?: throw Exception("Member created but no member_id returned")
            } else {
              val errorCodeElement = jsonResponse["error_code"]?.toString()
              val errorCode =
                errorCodeElement?.removeSurrounding("\"")?.removeSurrounding("JsonPrimitive(")?.removeSurrounding(")")
              val errorDetailsElement = jsonResponse["error_details"]?.toString()
              val errorDetails =
                errorDetailsElement?.removeSurrounding("\"")?.removeSurrounding("JsonPrimitive(")
                  ?.removeSurrounding(")")
              throw Exception("Member creation failed: ${errorCode ?: "UNKNOWN_ERROR"}${errorDetails?.let { " - $it" } ?: ""}")
            }
          } catch (e: Exception) {
            // Fallback: treat response as string and try manual parsing
            val responseString = responseData.toString()
            if (responseString.contains("\"success\":true") && responseString.contains("\"member_id\"")) {
              // Extract member_id using regex
              val memberIdRegex = "\"member_id\"\\s*:\\s*\"([^\"]+)\"".toRegex()
              val matchResult = memberIdRegex.find(responseString)
              matchResult?.groupValues?.get(1) ?: throw Exception("Could not extract member_id from response")
            } else {
              throw Exception("Member creation failed: ${responseString}")
            }
          }
        }
      emit(result)
    }

  @OptIn(SupabaseExperimental::class)
  override fun listenToAdminCountChanges(): Flow<Unit> {
    return merge(
      // Listen to organisational_member table (not member_in_organisation view)
      supabaseClient.from("organisational_member").selectAsFlow(
        primaryKeys = listOf(AdminTableChange::id)
      ),
      // Listen to arya_samaj table
      supabaseClient.from("arya_samaj").selectAsFlow(
        primaryKeys = listOf(AdminTableChange::id)
      ),
      // Listen to family table
      supabaseClient.from("family").selectAsFlow(
        primaryKeys = listOf(AdminTableChange::id)
      ),
      // Listen to family_member table (for family count changes)
      supabaseClient.from("family_member").selectAsFlow(
        primaryKeys = listOf(AdminTableChange::id)
      ),
      // Listen to member table (affects member_not_in_family view)
      supabaseClient.from("member").selectAsFlow(
        primaryKeys = listOf(AdminTableChange::id)
      )
    ).map { Unit }
  }
}

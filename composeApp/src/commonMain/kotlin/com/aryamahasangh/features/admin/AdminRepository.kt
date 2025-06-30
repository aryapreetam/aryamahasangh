
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

interface AdminRepository {
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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

  override suspend fun getOrganisationalMembers(): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(MembersInOrganisationQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          if (response.data?.memberInOrganisationCollection?.edges.isNullOrEmpty()) {
            throw Exception("No results found")
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

  override suspend fun getMembersCount(): Flow<Result<Long>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(MembersInOrganisationCountQuery()).execute()
//          var count = 0L
//          try {
//            count = supabaseClient.from("member_in_organisation").select { Count.EXACT }.countOrNull() ?: 0L
//          } catch (e: Exception) {
//            throw Exception("Unknown error occurred ${e.message}")
//          }

          response.data?.memberInOrganisationCollection?.totalCount?.toLong() ?: 0L
        }
      emit(result)
    }

  override suspend fun getMember(id: String): Flow<Result<MemberDetail>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(MemberDetailQuery(id)).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          val memberNode = response.data?.memberCollection?.edges?.firstOrNull()?.node
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
      emit(result)
    }

  override suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(SearchMembersQuery("%$query%")).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Update failed")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Address creation failed")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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
      val result =
        safeCall {
          val response = apolloClient.query(EkalAryaMembersQuery()).execute()
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
            )
          } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun searchEkalAryaMembers(query: String): Flow<Result<List<MemberShort>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(SearchEkalAryaMembersQuery("%$query%")).execute()
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
            )
          } ?: emptyList()
        }
      emit(result)
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Member creation failed")
          }

          // Parse the JSON response from insertMemberDetails function
          val jsonResponse = response.data?.insertMemberDetails
          val success = (jsonResponse as? Map<*, *>)?.get("success") as? Boolean ?: false
          
          if (!success) {
            val errorCode = (jsonResponse as? Map<*, *>)?.get("error_code") as? String ?: "UNKNOWN_ERROR"
            val errorDetails = (jsonResponse as? Map<*, *>)?.get("error_details") as? String
            throw Exception("Member creation failed: $errorCode ${errorDetails?.let { "- $it" } ?: ""}")
          }
          
          // Return the member ID from the successful response
          jsonResponse["member_id"] as? String ?: ""
        }
      emit(result)
    }
}

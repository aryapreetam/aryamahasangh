package org.aryamahasangh.features.admin

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.aryamahasangh.*
import org.aryamahasangh.components.AddressData
import org.aryamahasangh.components.AryaSamaj
import org.aryamahasangh.components.Gender
import org.aryamahasangh.components.getActiveImageUrls
import org.aryamahasangh.features.activities.LatLng
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.features.admin.data.AryaSamajDetail
import org.aryamahasangh.features.admin.data.AryaSamajFormData
import org.aryamahasangh.features.admin.data.AryaSamajListItem
import org.aryamahasangh.features.admin.data.AryaSamajMember
import org.aryamahasangh.network.supabaseClient
import org.aryamahasangh.type.GenderFilter
import org.aryamahasangh.type.SamajMemberInsertInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

interface AdminRepository {
  // Member methods
  suspend fun getMembers(): Flow<Result<List<MemberShort>>>
  suspend fun getMembersCount(): Flow<Result<Long>>
  suspend fun getMember(id: String): Flow<Result<MemberDetail>>
  suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>>
  
  // Search members that returns Member list for MembersComponent
  suspend fun searchMembersForSelection(query: String): Flow<Result<List<Member>>>

  suspend fun updateMemberDetails(
    memberId: String,
    name: String?,
    phoneNumber: String?,
    educationalQualification: String?,
    email: String?
  ): Flow<Result<Boolean>>

  suspend fun updateMemberPhoto(memberId: String, photoUrl: String): Flow<Result<Boolean>>
  
//  suspend fun addMember(
//    name: String,
//    phoneNumber: String,
//    educationalQualification: String?,
//    profileImageUrl: String?,
//    email: String?
//  ): Flow<Result<Boolean>>

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

  // New create member method with all fields
  suspend fun createMember(
    name: String,
    phoneNumber: String,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    educationalQualification: String?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImageUrl: String?,
    addressId: String,
    tempAddressId: String?,
    referrerId: String?,
    aryaSamajId: String?
  ): Flow<Result<String>>

  // AryaSamaj methods
  suspend fun getAllAryaSamajs(): Flow<Result<List<AryaSamaj>>>
  suspend fun searchAryaSamajs(query: String): Flow<Result<List<AryaSamaj>>>

  suspend fun deleteMember(memberId: String): Flow<Result<Boolean>>
}

class AdminRepositoryImpl(private val apolloClient: ApolloClient) : AdminRepository {
  override suspend fun getMembers(): Flow<Result<List<MemberShort>>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.query(MembersQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.memberCollection?.edges?.map {
        val member = it.node.memberShort
        val place = buildString {
          // TODO: Add district and state info when available from GraphQL
          // For now, we'll leave it empty
        }
        MemberShort(
          id = member.id,
          name = member.name!!,
          profileImage = member.profileImage ?: "",
          place = place
        )
      }!!
    }
    emit(result)
  }

  override suspend fun getMembersCount(): Flow<Result<Long>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      var count = 0L
      try {
        count = supabaseClient.from("member").select { count(Count.EXACT) }.countOrNull() ?: 0L
      }catch (e: Exception) {
        throw Exception("Unknown error occurred ${e.message}")
      }
      count
    }
    emit(result)
  }

  override suspend fun getMember(id: String): Flow<Result<MemberDetail>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.query(MemberDetailQuery(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      val members = response.data?.memberCollection
      val memberNode = members?.edges?.firstOrNull()?.node
      if (memberNode == null) {
        throw Exception("Member not found")
      }

      val organisations = memberNode.organisationalMemberCollection?.edges?.map {
        val organisation = it.node.organisation!!
        OrganisationInfo(
          id = organisation.id,
          name = organisation.name!!,
          logo = organisation.logo ?: "",
        )
      }
      val activities = memberNode.activityMemberCollection?.edges?.map {
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

      val address = memberNode.address
      val addressInfo = if (address != null) {
        "${address.basicAddress ?: ""}, ${address.district ?: ""}, ${address.state ?: ""} ${address.pincode ?: ""}"
      } else ""

      MemberDetail(
        id = memberNode.id,
        name = memberNode.name ?: "",
        profileImage = memberNode.profileImage ?: "",
        phoneNumber = memberNode.phoneNumber ?: "",
        educationalQualification = memberNode.educationalQualification ?: "",
        email = memberNode.email ?: "",
        address = addressInfo,
        district = address?.district ?: "",
        state = address?.state ?: "",
        pincode = address?.pincode ?: "",
        organisations = organisations ?: emptyList(),
        activities = activities ?: emptyList()
      )
    }
    emit(result)
  }

  override suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.query(SearchMembersQuery("%$query%")).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.memberCollection?.edges?.map {
        val member = it.node.memberShort
        val place = buildString {
          // TODO: Add district and state info when available from GraphQL
          // For now, we'll leave it empty
        }
        MemberShort(
          id = member.id,
          name = member.name!!,
          profileImage = member.profileImage ?: "",
          place = place
        )
      } ?: emptyList()
    }
    emit(result)
  }

  override suspend fun searchMembersForSelection(query: String): Flow<Result<List<Member>>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      // TODO decide later
//      val response = if (query.isBlank()) {
//        // Return all members when query is empty
//        apolloClient.query(MembersQuery()).execute()
//      } else {
//        apolloClient.query(SearchMembersQuery("%$query%")).execute()
//      }

      val response =
        apolloClient.query(SearchMembersQuery("%$query%")).execute()


      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
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

  override suspend fun updateMemberDetails(
    memberId: String,
    name: String?,
    phoneNumber: String?,
    educationalQualification: String?,
    email: String?
  ): Flow<Result<Boolean>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      // Now we can update all fields if name and phoneNumber are provided
      if (name != null && phoneNumber != null) {
        val response = apolloClient.mutation(
          UpdateMemberDetailsMutation(
            memberId = memberId,
            name = name,
            phoneNumber = phoneNumber,
            educationalQualification = Optional.presentIfNotNull(educationalQualification),
            email = Optional.presentIfNotNull(email)
          )
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
      }
      true
    }
    emit(result)
  }

  override suspend fun updateMemberPhoto(
    memberId: String,
    photoUrl: String
  ): Flow<Result<Boolean>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.mutation(
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

//  override suspend fun addMember(
//    name: String,
//    phoneNumber: String,
//    educationalQualification: String?,
//    profileImageUrl: String?,
//    email: String?
//  ): Flow<Result<Boolean>> = flow {
//    emit(Result.Loading)
//    val result = safeCall {
//      val response = apolloClient.mutation(
//        AddMemberMutation(
//          name = name,
//          phoneNumber = phoneNumber,
//          educationalQualification = Optional.presentIfNotNull(educationalQualification),
//          profileImageUrl = Optional.presentIfNotNull(profileImageUrl),
//          email = Optional.presentIfNotNull(email)
//        )
//      ).execute()
//      if (response.hasErrors()) {
//        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
//      }
//      true
//    }
//    emit(result)
//  }

  override suspend fun createAddress(
    basicAddress: String,
    state: String,
    district: String,
    pincode: String,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?
  ): Flow<Result<String>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.mutation(
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

  override suspend fun createMember(
    name: String,
    phoneNumber: String,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    educationalQualification: String?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImageUrl: String?,
    addressId: String,
    tempAddressId: String?,
    referrerId: String?,
    aryaSamajId: String?
  ): Flow<Result<String>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      // Convert Gender to GenderFilter
      val genderFilter = when (gender) {
        Gender.MALE -> GenderFilter.MALE
        Gender.FEMALE -> GenderFilter.FEMALE
        Gender.OTHER -> GenderFilter.ANY  // Map OTHER to ANY since GenderFilter doesn't have OTHER
        null -> null
      }

      val response = apolloClient.mutation(
        CreateMemberMutation(
          name = name,
          phoneNumber = phoneNumber,
          email = Optional.presentIfNotNull(email),
          dob = Optional.presentIfNotNull(dob),
          gender = Optional.presentIfNotNull(genderFilter),
          educationalQualification = Optional.presentIfNotNull(educationalQualification),
          occupation = Optional.presentIfNotNull(occupation),
          joiningDate = Optional.presentIfNotNull(joiningDate),
          introduction = Optional.presentIfNotNull(introduction),
          profileImage = Optional.presentIfNotNull(profileImageUrl),
          addressId = Optional.Present(addressId),
          tempAddressId = Optional.presentIfNotNull(tempAddressId),
          referrerId = Optional.presentIfNotNull(referrerId),
          aryaSamajId = Optional.presentIfNotNull(aryaSamajId)
        )
      ).execute()

      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Member creation failed")
      }

      val memberId = response.data?.insertIntoMemberCollection?.records?.firstOrNull()?.id
      if (memberId == null) {
        throw Exception("Member creation failed - no ID returned")
      }

      memberId
    }
    emit(result)
  }

  override suspend fun getAllAryaSamajs(): Flow<Result<List<AryaSamaj>>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.query(AryaSamajsQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.aryaSamajCollection?.edges?.map { edge ->
        val node = edge.node
        val aryaSamajFields = node.aryaSamajFields
        val address = node.address?.addressFields

        AryaSamaj(
          id = aryaSamajFields.id,
          name = aryaSamajFields.name ?: "",
          address = "${address?.basicAddress ?: ""}, ${address?.district ?: ""}, ${address?.state ?: ""} ${address?.pincode ?: ""}".trim(),
          district = address?.district ?: ""
        )
      } ?: emptyList()
    }
    emit(result)
  }

  override suspend fun searchAryaSamajs(query: String): Flow<Result<List<AryaSamaj>>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      // For now, just return all AryaSamajs and filter client-side
      // You can implement a proper search query later if needed
      val response = apolloClient.query(AryaSamajsQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      val allAryaSamajs = response.data?.aryaSamajCollection?.edges?.map { edge ->
        val node = edge.node
        val aryaSamajFields = node.aryaSamajFields
        val address = node.address?.addressFields

        AryaSamaj(
          id = aryaSamajFields.id,
          name = aryaSamajFields.name ?: "",
          address = "${address?.basicAddress ?: ""}, ${address?.district ?: ""}, ${address?.state ?: ""} ${address?.pincode ?: ""}".trim(),
          district = address?.district ?: ""
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

  override suspend fun deleteMember(memberId: String): Flow<Result<Boolean>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.mutation(DeleteMemberMutation(memberId)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      true
    }
    emit(result)
  }
}

package org.aryamahasangh.features.admin

import com.apollographql.apollo.ApolloClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.aryamahasangh.*
import org.aryamahasangh.components.AddressData
import org.aryamahasangh.components.getActiveImageUrls
import org.aryamahasangh.features.activities.LatLng
import org.aryamahasangh.features.admin.data.AryaSamajDetail
import org.aryamahasangh.features.admin.data.AryaSamajFormData
import org.aryamahasangh.features.admin.data.AryaSamajListItem
import org.aryamahasangh.features.admin.data.AryaSamajMember
import org.aryamahasangh.network.supabaseClient
import org.aryamahasangh.type.SamajMemberInsertInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

interface AdminRepository {
  // Member methods
  suspend fun getMembers(): Flow<Result<List<MemberShort>>>
  suspend fun getMembersCount(): Flow<Result<Long>>
  suspend fun getMember(id: String): Flow<Result<MemberDetail>>
  suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>>
  suspend fun updateMemberDetails(
    memberId: String,
    name: String?,
    phoneNumber: String?,
    educationalQualification: String?,
    email: String?
  ): Flow<Result<Boolean>>

  suspend fun updateMemberPhoto(memberId: String, photoUrl: String): Flow<Result<Boolean>>
  suspend fun addMember(
    name: String,
    phoneNumber: String,
    educationalQualification: String?,
    profileImageUrl: String?,
    email: String?
  ): Flow<Result<Boolean>>

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
            educationalQualification = com.apollographql.apollo.api.Optional.presentIfNotNull(educationalQualification),
            email = com.apollographql.apollo.api.Optional.presentIfNotNull(email)
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

  override suspend fun addMember(
    name: String,
    phoneNumber: String,
    educationalQualification: String?,
    profileImageUrl: String?,
    email: String?
  ): Flow<Result<Boolean>> = flow {
    emit(Result.Loading)
    val result = safeCall {
      val response = apolloClient.mutation(
        AddMemberMutation(
          name = name,
          phoneNumber = phoneNumber,
          educationalQualification = educationalQualification ?: "",
          profileImageUrl = profileImageUrl ?: "",
          email = com.apollographql.apollo.api.Optional.presentIfNotNull(email)
        )
      ).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      true
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

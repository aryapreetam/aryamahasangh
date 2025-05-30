package org.aryamahasangh.features.admin

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.MembersQuery
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

interface AdminRepository {
  suspend fun getMembers(): Flow<Result<List<MemberShort>>>
  suspend fun getMember(id: String): Flow<Result<Member>>
  suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>>
  suspend fun updateMemberDetails(memberId: String, name: String, phoneNumber: String, educationalQualification: String)
  suspend fun updateMemberPhoto(memberId: String, photoUrl: String): Flow<Result<Boolean>>
  suspend fun addMember(
    name: String,
    phoneNumber: String,
    educationalQualification: String,
    profileImageUrl: String,
    email: String
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
        MemberShort(
          id = member.id,
          name = member.name!!,
          profileImage = member.profile_image ?: ""
        )
      }!!
    }
    emit(result)
  }

  override suspend fun getMember(id: String): Flow<Result<Member>> {
    // apolloClient.query(MemberDetailQuery(id)).execute()
    TODO("Not yet implemented")
  }

  override suspend fun searchMembers(query: String): Flow<Result<List<MemberShort>>> {
    //  apolloClient.query(SearchMembersQuery(query)).execute()
    TODO("Not yet implemented")
  }

  override suspend fun updateMemberDetails(
    memberId: String,
    name: String,
    phoneNumber: String,
    educationalQualification: String
  ) {
    // apolloClient.mutation(UpdateMemberDetailsMutation(memberId, name, phoneNumber, "")).execute()
    TODO("Not yet implemented")
  }

  override suspend fun updateMemberPhoto(
    memberId: String,
    photoUrl: String
  ): Flow<Result<Boolean>> {
    // apolloClient.mutation(UpdateMemberPhotoMutation(memberId, photoUrl)).execute()
    TODO("Not yet implemented")
  }

  override suspend fun addMember(
    name: String,
    phoneNumber: String,
    educationalQualification: String,
    profileImageUrl: String,
    email: String
  ): Flow<Result<Boolean>> {
    // apolloClient.mutation(AddMemberMutation(name, phoneNumber, educationalQualification, profileImageUrl, "")).execute()
    TODO("Not yet implemented")
  }

  override suspend fun deleteMember(memberId: String): Flow<Result<Boolean>> {
    // apolloClient.mutation(DeleteMemberMutation(memberId)).execute()
    TODO("Not yet implemented")
  }
}

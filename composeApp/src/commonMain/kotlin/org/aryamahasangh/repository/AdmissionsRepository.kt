package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.AddStudentAdmissionDataMutation
import org.aryamahasangh.StudentApplicationsQuery
import org.aryamahasangh.type.AdmissionFormDataInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling admissions-related operations
 */
interface AdmissionsRepository {
  /**
   * Get all student applications
   */
  fun getStudentApplications(): Flow<Result<List<StudentApplicationsQuery.StudentsApplied>>>

  /**
   * Submit a student admission form
   */
  suspend fun submitAdmissionForm(input: AdmissionFormDataInput): Result<Boolean>
}

/**
 * Implementation of AdmissionsRepository that uses Apollo GraphQL client
 */
class AdmissionsRepositoryImpl(private val apolloClient: ApolloClient) : AdmissionsRepository {

  override fun getStudentApplications(): Flow<Result<List<StudentApplicationsQuery.StudentsApplied>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(StudentApplicationsQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.studentsApplied ?: emptyList()
    }

    emit(result)
  }

  override suspend fun submitAdmissionForm(input: AdmissionFormDataInput): Result<Boolean> {
    return safeCall {
      val response = apolloClient.mutation(AddStudentAdmissionDataMutation(input)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.addStudentAdmissionData ?: false
    }
  }
}

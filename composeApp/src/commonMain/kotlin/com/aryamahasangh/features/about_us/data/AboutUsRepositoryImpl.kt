package com.aryamahasangh.features.about_us.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.aryamahasangh.OrganisationNamesQuery
import com.aryamahasangh.OrganisationQuery
import com.aryamahasangh.features.about_us.domain.repository.AboutUsRepository
import com.aryamahasangh.features.about_us.domain.repository.OrganisationName
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.type.OrganisationFilter
import com.aryamahasangh.type.StringFilter
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AboutUsRepositoryImpl(private val apolloClient: ApolloClient) : AboutUsRepository {
//  override fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>> =
//    flow {
//      emit(Result.Loading)
//
//      val result =
//        safeCall {
//          val response = apolloClient.query(OrganisationByNameQuery(name)).execute()
//
//          if (response.hasErrors()) {
//            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
//            throw Exception(errorMessage)
//          }
//          val org = response.data?.organisation?.first()!!
//          OrganisationDetail(
//            id = org.id,
//            name = org.name,
//            description = org.description,
//            logo = org.logo
//          )
//        }
//      emit(result)
//    }
//
//  override fun getOrganisationNames(): Flow<Result<List<OrganisationName>>> =
//    flow {
//      emit(Result.Loading)
//
//      val result =
//        safeCall {
//          val response = apolloClient.query(OrganisationNamesQuery()).execute()
//
//          if (response.hasErrors()) {
//            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
//            throw Exception(errorMessage)
//          }
//          response.data?.organisation?.map { edge ->
//            OrganisationName(
//              id = edge.id,
//              name = edge.name
//            )
//          } ?: emptyList()
//        }
//
//      emit(result)
//    }

  override fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationQuery(
            filter =
              Optional.present(
                OrganisationFilter(name = Optional.present(StringFilter(eq = Optional.present(name))))
              )
          )).execute()

          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            throw Exception(errorMessage)
          }
          val org = response.data?.organisationCollection?.edges?.first()!!.node
          OrganisationDetail(
            id = org.id,
            name = org.name,
            description = org.description,
            logo = org.logo
          )
        }
      emit(result)
    }

  override fun getOrganisationNames(): Flow<Result<List<OrganisationName>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationNamesQuery()).execute()

          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            throw Exception(errorMessage)
          }
          response.data?.organisationCollection?.edges?.map { edge ->

            OrganisationName(
              id = edge.node.id,
              name = edge.node.name
            )
          } ?: emptyList()
        }

      emit(result)
    }
}

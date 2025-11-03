package com.aryamahasangh.features.about_us.domain.repository

import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing organization name and id
 */
data class OrganisationName(
  val id: String,
  val name: String
)

/**
 * Repository for handling about us related operations
 */
interface AboutUsRepository {
  /**
   * Get organisation details by name
   */
  fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>>

  /**
   * Get all organisation names
   */
  fun getOrganisationNames(): Flow<Result<List<OrganisationName>>>
}

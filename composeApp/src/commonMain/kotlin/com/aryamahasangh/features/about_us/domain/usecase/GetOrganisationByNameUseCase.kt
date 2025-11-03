package com.aryamahasangh.features.about_us.domain.usecase

import com.aryamahasangh.features.about_us.domain.repository.AboutUsRepository
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow

class GetOrganisationByNameUseCase(
  private val repository: AboutUsRepository
) {
  operator fun invoke(name: String): Flow<Result<OrganisationDetail>> =
    repository.getOrganisationByName(name)
}
